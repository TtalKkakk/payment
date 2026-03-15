package com.example.pg.merchant.command.application;

import com.example.pg.merchant.command.application.dto.RegenerateSecretResultDto;
import com.example.pg.merchant.domain.aggregate.Merchant;
import com.example.pg.merchant.domain.event.MerchantActivatedEvent;
import com.example.pg.merchant.domain.event.MerchantDeletedEvent;
import com.example.pg.merchant.domain.event.MerchantSecretRegeneratedEvent;
import com.example.pg.merchant.domain.event.MerchantSuspendedEvent;
import com.example.pg.merchant.infrastructure.persistence.MerchantRepository;
import com.example.pg.merchantapplication.presentation.port.MerchantApplicationPort;
import com.example.pg.common.exception.BusinessException;
import com.example.pg.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantService {

    private final MerchantRepository merchantRepository;
    private final MerchantApplicationPort merchantApplicationPort;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * @param merchantId
     * @return RegenerateSecretResultDto
     * X-API-KEY 및 X-API-SECRET 재생성
     */
    @Transactional
    public RegenerateSecretResultDto regenerateSecret(String merchantId) {
        log.debug("[Merchant] regenerateSecret start merchantId={}", merchantId);
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MERCHANT_NOT_FOUND, merchantId));

        merchant.regenerateSecret();

        applicationEventPublisher.publishEvent(MerchantSecretRegeneratedEvent.from(merchant.getId()));

        log.debug("[Merchant] regenerateSecret committed merchantId={}", merchantId);
        return new RegenerateSecretResultDto(merchant.getApiKey(), merchant.getApiSecret());
    }

    /**
     * 가맹점 탈퇴(soft delete). 상태를 WITHDRAWN으로 변경하고, 같은 트랜잭션에서 신청서를 SUBSCRIPTION_ENDED로 전이한 뒤 이벤트를 발행한다.
     */
    @Transactional
    public void deleteMerchant(String merchantId) {
        log.debug("[Merchant] deleteMerchant start merchantId={}", merchantId);
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MERCHANT_NOT_FOUND, merchantId));

        merchant.withdraw();
        merchantRepository.save(merchant);

        merchantApplicationPort.markSubscriptionEnded(merchant.getApplicationId());

        applicationEventPublisher.publishEvent(MerchantDeletedEvent.from(merchantId, merchant.getName(), merchant.getApplicationId()));
        log.debug("[Merchant] deleteMerchant committed merchantId={}", merchantId);
    }

    /**
     * 가맹점 API 정지. ACTIVE → SUSPENDED.
     */
    @Transactional
    public void suspendMerchant(String merchantId) {
        log.debug("[Merchant] suspendMerchant start merchantId={}", merchantId);
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MERCHANT_NOT_FOUND, merchantId));
        merchant.suspend();
        merchantRepository.save(merchant);
        applicationEventPublisher.publishEvent(MerchantSuspendedEvent.from(merchantId, merchant.getName()));
        log.debug("[Merchant] suspendMerchant committed merchantId={}", merchantId);
    }

    /**
     * 가맹점 정지 해제. SUSPENDED → ACTIVE.
     */
    @Transactional
    public void activateMerchant(String merchantId) {
        log.debug("[Merchant] activateMerchant start merchantId={}", merchantId);
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MERCHANT_NOT_FOUND, merchantId));
        merchant.activate();
        merchantRepository.save(merchant);
        applicationEventPublisher.publishEvent(MerchantActivatedEvent.from(merchantId, merchant.getName()));
        log.debug("[Merchant] activateMerchant committed merchantId={}", merchantId);
    }

    /**
     * 관리자용: 정지(SUSPENDED) 상태인 가맹점만 탈퇴(WITHDRAWN) 처리.
     * 같은 트랜잭션에서 신청서를 SUBSCRIPTION_ENDED로 전이한 뒤 MerchantDeletedEvent 발행.
     */
    @Transactional
    public void withdrawMerchantFromSuspended(String merchantId) {
        log.debug("[Merchant] withdrawMerchantFromSuspended start merchantId={}", merchantId);
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MERCHANT_NOT_FOUND, merchantId));
        merchant.withdrawFromSuspended();
        merchantRepository.save(merchant);

        merchantApplicationPort.markSubscriptionEnded(merchant.getApplicationId());

        applicationEventPublisher.publishEvent(MerchantDeletedEvent.from(merchantId, merchant.getName(), merchant.getApplicationId()));
        log.debug("[Merchant] withdrawMerchantFromSuspended committed merchantId={}", merchantId);
    }
}
