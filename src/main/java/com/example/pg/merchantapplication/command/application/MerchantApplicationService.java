package com.example.pg.merchantapplication.command.application;

import com.example.pg.merchantapplication.command.application.port.MerchantApplicationPort;
import com.example.pg.merchantapplication.domain.aggregate.MerchantApplication;
import com.example.pg.merchantapplication.domain.enumerate.MerchantApplicationStatus;
import com.example.pg.merchantapplication.domain.vo.ApplicationName;
import com.example.pg.merchantapplication.domain.vo.BusinessNumber;
import com.example.pg.merchantapplication.domain.vo.ContactEmail;
import com.example.pg.merchantapplication.domain.vo.ContactPhone;
import com.example.pg.merchantapplication.domain.vo.PasswordHash;
import com.example.pg.merchantapplication.domain.event.MerchantApplicationApprovedEvent;
import com.example.pg.merchantapplication.domain.event.MerchantApplicationCancelledEvent;
import com.example.pg.merchantapplication.domain.event.MerchantApplicationRejectedEvent;
import com.example.pg.merchantapplication.domain.event.MerchantApplicationPendedEvent;
import com.example.pg.merchantapplication.domain.event.MerchantApplicationSubscriptionEndedEvent;
import com.example.pg.merchantapplication.infrastructure.persistence.MerchantApplicationRepository;
import com.example.pg.common.exception.BusinessException;
import com.example.pg.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantApplicationService {

    private final MerchantApplicationRepository merchantApplicationRepository;
    private final MerchantApplicationPort merchantApplicationPort;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final PasswordEncoder passwordEncoder;

    /**
     *
     * @param name
     * @param businessNumber
     * @param contactPhone
     * @param contactEmail
     * @param rawPassword
     * @return MerchantApplication
     * MerchantApplication.Status == CANCELLED OR SUBSCRIPTION_ENDED OR REJECTED
     * MerchantApplication가 soft delete된 엔터티가 존재하면 reapply(update)
     * 그렇지 않으면 save(create)
     */
    @Transactional
    public MerchantApplication apply(String name, String businessNumber, String contactPhone, String contactEmail, String rawPassword) {
        log.debug("[MerchantApplication] apply start businessNumber={} name={}", businessNumber, name);

        if (merchantApplicationRepository.existsByBusinessNumberAndStatusIn(
                businessNumber,
                List.of(MerchantApplicationStatus.PENDING, MerchantApplicationStatus.APPROVED))
        ) {
            throw new BusinessException(ErrorCode.DUPLICATE_BUSINESS_NUMBER);
        }

        String passwordHash = passwordEncoder.encode(rawPassword);
        MerchantApplication merchantApplication = merchantApplicationRepository.findByBusinessNumber(businessNumber)
                .filter(app -> app.getStatus() == MerchantApplicationStatus.CANCELLED
                        || app.getStatus() == MerchantApplicationStatus.SUBSCRIPTION_ENDED
                        || app.getStatus() == MerchantApplicationStatus.REJECTED)
                .map(app -> {
                    app.reapply(
                            ApplicationName.of(name),
                            ContactPhone.of(contactPhone),
                            ContactEmail.of(contactEmail),
                            PasswordHash.of(passwordHash)
                    );
                    return app;
                })
                .orElseGet(() -> MerchantApplication.create(
                        ApplicationName.of(name),
                        BusinessNumber.of(businessNumber),
                        ContactPhone.of(contactPhone),
                        ContactEmail.of(contactEmail),
                        PasswordHash.of(passwordHash)
                ));
        merchantApplicationRepository.save(merchantApplication);

        applicationEventPublisher.publishEvent(MerchantApplicationPendedEvent.from(
                merchantApplication.getId(),
                merchantApplication.getName(),
                merchantApplication.getBusinessNumber(),
                merchantApplication.getContactEmail())
        );
        log.debug("[MerchantApplication] apply committed applicationId={}", merchantApplication.getId());
        return merchantApplication;
    }

    /**
     * 사업자번호 + 비밀번호로 신청 취소. PENDING인 신청만 CANCELLED로 전이.
     */
    @Transactional
    public void deleteByBusinessNumberAndPassword(String businessNumber, String rawPassword) {
        log.debug("[MerchantApplication] deleteByBusinessNumber start businessNumber={}", businessNumber);
        MerchantApplication application = merchantApplicationRepository.findByBusinessNumber(businessNumber)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICATION_PASSWORD_MISMATCH));
        if (application.getStatus() != MerchantApplicationStatus.PENDING) {
            throw new BusinessException(ErrorCode.APPLICATION_ALREADY_PROCESSED, application.getStatus().name());
        }
        if (!passwordEncoder.matches(rawPassword, application.getPasswordHash())) {
            throw new BusinessException(ErrorCode.APPLICATION_PASSWORD_MISMATCH);
        }
        application.cancel();
        merchantApplicationRepository.save(application);
        applicationEventPublisher.publishEvent(MerchantApplicationCancelledEvent.from(
                application.getId(),
                application.getName(),
                businessNumber
        ));
        log.debug("[MerchantApplication] deleteByBusinessNumber committed businessNumber={}", businessNumber);
    }

    @Transactional
    public void approve(String applicationId) {
        log.debug("[MerchantApplication] approve start applicationId={}", applicationId);
        MerchantApplication merchantApplication = merchantApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MERCHANT_APPLICATION_NOT_FOUND, applicationId));

        merchantApplication.approve();
        merchantApplicationRepository.save(merchantApplication);

        merchantApplicationPort.createFromApprovedApplication(
                merchantApplication.getId(),
                merchantApplication.getName()
        );

        applicationEventPublisher.publishEvent(MerchantApplicationApprovedEvent.from(
                merchantApplication.getId(),
                merchantApplication.getName())
        );
        log.debug("[MerchantApplication] approve committed applicationId={}", applicationId);
    }

    @Transactional
    public void reject(String applicationId, String reason) {
        log.debug("[MerchantApplication] reject start applicationId={} reasonLength={}", applicationId, reason != null ? reason.length() : 0);
        MerchantApplication merchantApplication = merchantApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MERCHANT_APPLICATION_NOT_FOUND, applicationId));

        merchantApplication.reject(reason);
        merchantApplicationRepository.save(merchantApplication);

        applicationEventPublisher.publishEvent(MerchantApplicationRejectedEvent.from(
                merchantApplication.getId(), merchantApplication.getName(), reason));
        log.debug("[MerchantApplication] reject committed applicationId={} reasonLength={}", applicationId, reason != null ? reason.length() : 0);
    }

    /**
     * 구독 종료로 전이. Merchant 도메인에서 탈퇴 시 포트를 통해 호출.
     */
    @Transactional
    public void markSubscriptionEnded(String applicationId) {
        log.debug("[MerchantApplication] markSubscriptionEnded start applicationId={}", applicationId);
        MerchantApplication application = merchantApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MERCHANT_APPLICATION_NOT_FOUND, applicationId));
        MerchantApplicationStatus before = application.getStatus();
        application.subscriptionEnded();
        merchantApplicationRepository.save(application);
        applicationEventPublisher.publishEvent(MerchantApplicationSubscriptionEndedEvent.from(
                applicationId,
                application.getName(),
                before != null ? before.name() : null
        ));
        log.debug("[MerchantApplication] markSubscriptionEnded committed applicationId={}", applicationId);
    }
}
