package com.example.pg.cardcompany.application;

import com.example.pg.cardcompany.domain.event.BillingKeyIssuedEvent;
import com.example.pg.cardcompany.domain.event.BillingKeysRevokedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 카드사 도메인 이벤트 구독자.
 * 트랜잭션 커밋 후 실행. 추후 감사 로그, 정산 반영, 외부 알림 등으로 확장 가능.
 */
@Slf4j
@Component
public class CardCompanyEventListener {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBillingKeyIssued(BillingKeyIssuedEvent event) {
        log.info("[CardCompanyEvent] BillingKeyIssued merchantId={}, cardToken={}, occurredAt={}",
                event.merchantId(), event.cardToken(), event.occurredAt());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBillingKeysRevoked(BillingKeysRevokedEvent event) {
        log.info("[CardCompanyEvent] BillingKeysRevoked merchantId={}, occurredAt={}",
                event.merchantId(), event.occurredAt());
    }
}
