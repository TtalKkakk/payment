package com.example.pg.merchant.command.application;

import com.example.pg.merchant.command.application.dto.RegenerateSecretResult;
import com.example.pg.merchant.domain.aggregate.Merchant;
import com.example.pg.merchant.domain.event.MerchantDeletedEvent;
import com.example.pg.merchant.domain.event.MerchantSecretRegeneratedEvent;
import com.example.pg.merchant.infrastructure.persistence.MerchantRepository;
import com.example.pg.merchantapplication.command.application.port.MerchantApplicationPort;
import com.example.pg.exception.BusinessException;
import com.example.pg.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MerchantService {

    private final MerchantRepository merchantRepository;
    private final MerchantApplicationPort merchantApplicationPort;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public RegenerateSecretResult regenerateSecret(String merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MERCHANT_NOT_FOUND, merchantId));

        merchant.regenerateSecret();

        applicationEventPublisher.publishEvent(MerchantSecretRegeneratedEvent.from(merchant.getId()));

        return new RegenerateSecretResult(merchant.getApiKey(), merchant.getApiSecret());
    }

    /**
     * 가맹점 탈퇴(soft delete). 상태를 WITHDRAWN으로 변경하고, 같은 트랜잭션에서 신청서를 SUBSCRIPTION_ENDED로 전이한 뒤 이벤트를 발행한다.
     */
    @Transactional
    public void deleteMerchant(String merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MERCHANT_NOT_FOUND, merchantId));

        merchant.withdraw();
        merchantRepository.save(merchant);

        merchantApplicationPort.markSubscriptionEnded(merchant.getApplicationId());

        applicationEventPublisher.publishEvent(MerchantDeletedEvent.from(merchantId, merchant.getName(), merchant.getApplicationId()));
    }

    /**
     * 가맹점 API 정지. ACTIVE → SUSPENDED.
     */
    @Transactional
    public void suspendMerchant(String merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MERCHANT_NOT_FOUND, merchantId));
        merchant.suspend();
        merchantRepository.save(merchant);
    }

    /**
     * 가맹점 정지 해제. SUSPENDED → ACTIVE.
     */
    @Transactional
    public void activateMerchant(String merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MERCHANT_NOT_FOUND, merchantId));
        merchant.activate();
        merchantRepository.save(merchant);
    }

    /**
     * 관리자용: 정지(SUSPENDED) 상태인 가맹점만 탈퇴(WITHDRAWN) 처리.
     * 같은 트랜잭션에서 신청서를 SUBSCRIPTION_ENDED로 전이한 뒤 MerchantDeletedEvent 발행.
     */
    @Transactional
    public void withdrawMerchantFromSuspended(String merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MERCHANT_NOT_FOUND, merchantId));
        merchant.withdrawFromSuspended();
        merchantRepository.save(merchant);

        merchantApplicationPort.markSubscriptionEnded(merchant.getApplicationId());

        applicationEventPublisher.publishEvent(MerchantDeletedEvent.from(merchantId, merchant.getName(), merchant.getApplicationId()));
    }
}
