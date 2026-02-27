package com.example.pg.card.command.application;

import com.example.pg.card.domain.event.BillingKeyIssuedEvent;
import com.example.pg.card.domain.event.CardRegisteredEvent;
import com.example.pg.card.domain.repository.AuthCodeRepository;
import com.example.pg.card.domain.repository.CardRegistrationTokenRepository;
import com.example.pg.card.command.application.port.BillingKeyPort;
import com.example.pg.merchant.domain.event.MerchantDeletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

/**
 * Card 도메인 이벤트 구독자.
 * 트랜잭션 커밋 후 실행된다.
 * Merchant 삭제 시 해당 가맹점의 카드 등록 토큰·AuthCode를 정리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CardDomainEventListener {

    private final CardRegistrationTokenRepository tokenRepository;
    private final AuthCodeRepository authCodeRepository;
    private final BillingKeyPort billingKeyPort;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCardRegistered(CardRegisteredEvent event) {
        log.info("[DomainEvent] CardRegistered merchantId={}, ownerId={}, maskedNumber={}, occurredAt={}",
                event.merchantId(), event.ownerId(), event.maskedNumber(), event.occurredAt());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBillingKeyIssued(BillingKeyIssuedEvent event) {
        log.info("[DomainEvent] BillingKeyIssued merchantId={}, ownerId={}, maskedNumber={}, occurredAt={}",
                event.merchantId(), event.ownerId(), event.maskedNumber(), event.occurredAt());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onMerchantDeleted(MerchantDeletedEvent event) {
        String merchantId = event.merchantId();
        log.info("[DomainEvent] MerchantDeleted - Card 쪽 정리 merchantId={}, name={}", merchantId, event.name());
        tokenRepository.deleteByMerchantId(merchantId);
        authCodeRepository.deleteByMerchantId(merchantId);
        billingKeyPort.revokeBillingKeysByMerchantId(merchantId);
    }
}
