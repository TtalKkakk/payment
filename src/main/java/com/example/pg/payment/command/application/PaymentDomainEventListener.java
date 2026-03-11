package com.example.pg.payment.command.application;

import com.example.pg.payment.command.domain.enumerate.PaymentStatus;
import com.example.pg.payment.command.domain.event.PaymentCreatedEvent;
import com.example.pg.payment.command.domain.event.PaymentStatusChangedEvent;
import com.example.pg.payment.command.infrastructure.persistence.PaymentRepository;
import com.example.pg.payment.command.domain.vo.PaymentId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

/**
 * Payment 도메인 이벤트 구독자.
 * 트랜잭션 커밋 후 실행되므로, 실패 시 부수 효과가 발생하지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentDomainEventListener {

    private final PaymentRepository paymentRepository;
    private final PaymentWebhookService paymentWebhookService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentCreated(PaymentCreatedEvent event) {
        log.info("[DomainEvent] PaymentCreated paymentId={}, merchantId={}, amount={}, occurredAt={}",
                event.paymentId(), event.merchantId(), event.amount(), event.occurredAt());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void onPaymentStatusChanged(PaymentStatusChangedEvent event) {
        log.info("[DomainEvent] PaymentStatusChanged paymentId={}, status={}, occurredAt={}",
                event.paymentId(), event.status(), event.occurredAt());

        if (event.status() != PaymentStatus.AUTHORIZED
                && event.status() != PaymentStatus.FAILED
                && event.status() != PaymentStatus.CANCELED) {
            return;
        }

        paymentRepository.load(PaymentId.from(event.paymentId()))
                .ifPresent(paymentWebhookService::sendWebhook);
    }
}
