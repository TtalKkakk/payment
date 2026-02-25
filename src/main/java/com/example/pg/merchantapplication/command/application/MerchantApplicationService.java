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
import com.example.pg.merchantapplication.domain.event.MerchantApplicationRejectedEvent;
import com.example.pg.merchantapplication.domain.event.MerchantApplicationSubmittedEvent;
import com.example.pg.merchantapplication.infrastructure.persistence.MerchantApplicationRepository;
import com.example.pg.exception.BusinessException;
import com.example.pg.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MerchantApplicationService {

    private final MerchantApplicationRepository merchantApplicationRepository;
    private final MerchantApplicationPort merchantApplicationPort;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public MerchantApplication apply(String name, String businessNumber, String contactPhone, String contactEmail, String rawPassword) {
        if (merchantApplicationRepository.existsByBusinessNumberAndStatusIn(businessNumber,
                List.of(MerchantApplicationStatus.PENDING, MerchantApplicationStatus.APPROVED))) {
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

        applicationEventPublisher.publishEvent(MerchantApplicationSubmittedEvent.from(
                merchantApplication.getId(),
                merchantApplication.getName(),
                merchantApplication.getBusinessNumber(),
                merchantApplication.getContactEmail())
        );

        return merchantApplication;
    }

    /**
     * 사업자번호 + 비밀번호로 신청 취소. PENDING인 신청만 CANCELLED로 전이.
     */
    @Transactional
    public void deleteByBusinessNumberAndPassword(String businessNumber, String rawPassword) {
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
    }

    @Transactional
    public void approve(String applicationId) {
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
    }

    @Transactional
    public void reject(String applicationId, String reason) {
        MerchantApplication merchantApplication = merchantApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MERCHANT_APPLICATION_NOT_FOUND, applicationId));

        merchantApplication.reject(reason);
        merchantApplicationRepository.save(merchantApplication);

        applicationEventPublisher.publishEvent(MerchantApplicationRejectedEvent.from(
                merchantApplication.getId(), merchantApplication.getName(), reason));
    }
}
