package com.example.pg.merchant.command.application;

import com.example.pg.merchant.domain.event.MerchantActivatedEvent;
import com.example.pg.merchant.domain.event.MerchantDeletedEvent;
import com.example.pg.merchant.domain.event.MerchantSecretRegeneratedEvent;
import com.example.pg.merchant.domain.event.MerchantSuspendedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

/**
 * Merchant 도메인 이벤트 구독자.
 * 트랜잭션 커밋 후 실행되므로, 실패 시 부수 효과가 발생하지 않는다.
 */
@Slf4j
@Component
public class MerchantEventListener {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMerchantSecretRegenerated(MerchantSecretRegeneratedEvent event) {
        log.info("[Merchant] event=SecretRegenerated merchantId={} occurredAt={}",
                event.merchantId(), event.occurredAt());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMerchantDeleted(MerchantDeletedEvent event) {
        log.info("[Merchant] event=Deleted merchantId={} name={} applicationId={} occurredAt={}",
                event.merchantId(), event.name(), event.applicationId(), event.occurredAt());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMerchantSuspended(MerchantSuspendedEvent event) {
        log.info("[Merchant] event=Suspended merchantId={} name={} occurredAt={}",
                event.merchantId(), event.name(), event.occurredAt());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMerchantActivated(MerchantActivatedEvent event) {
        log.info("[Merchant] event=Activated merchantId={} name={} occurredAt={}",
                event.merchantId(), event.name(), event.occurredAt());
    }
}
