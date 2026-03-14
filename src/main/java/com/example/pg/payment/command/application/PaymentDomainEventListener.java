package com.example.pg.payment.command.application;

import com.example.pg.payment.domain.enumerate.PaymentStatus;
import com.example.pg.payment.domain.event.AuthorizationStartedEvent;
import com.example.pg.payment.domain.event.PaymentCreatedEvent;
import com.example.pg.payment.domain.event.PaymentStatusChangedEvent;
import com.example.pg.payment.domain.vo.PaymentId;
import com.example.pg.payment.infrastructure.persistence.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

/**
 * Payment 도메인 이벤트 구독자.
 * 트랜잭션 커밋 후 실행되므로, 실패 시 부수 효과가 발생하지 않는다.
 * 영수증 발급은 receipt 모듈의 ReceiptOnPaymentApprovedListener에서 처리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentDomainEventListener {

    private final PaymentRepository paymentRepository;
    private final PaymentWebhookService paymentWebhookService;
    private final PaymentAuthorizationProcessor paymentAuthorizationProcessor;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAuthorizationStarted(AuthorizationStartedEvent event) {
        paymentAuthorizationProcessor.processAuthorization(event.paymentId(), event.billingKey());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentCreated(PaymentCreatedEvent event) {
        log.info("[DomainEvent] PaymentCreated paymentId={}, merchantId={}, amount={}, occurredAt={}",
                event.paymentId(), event.merchantId(), event.amount(), event.occurredAt());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentStatusChanged(PaymentStatusChangedEvent event) {
        log.info("[DomainEvent] PaymentStatusChanged paymentId={}, status={}, occurredAt={}",
                event.paymentId(), event.status(), event.occurredAt());

        if (event.status() != PaymentStatus.FAILED && event.status() != PaymentStatus.CANCELED) {
            return;
        }

        paymentRepository.load(PaymentId.from(event.paymentId()))
                .ifPresent(paymentWebhookService::sendWebhook);
    }
}
