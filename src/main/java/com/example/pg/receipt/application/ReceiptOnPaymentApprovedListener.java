package com.example.pg.receipt.application;

import com.example.pg.payment.domain.enumerate.PaymentStatus;
import com.example.pg.payment.domain.event.PaymentStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

/**
 * 결제 승인 완료(AUTHORIZED) 시 영수증을 발급하는 리스너.
 * 결제 트랜잭션 커밋 후 별도 트랜잭션으로 실행되므로,
 * 영수증 생성이 실패해도 결제 승인은 롤백되지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReceiptOnPaymentApprovedListener {

    private final ReceiptService receiptService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onPaymentStatusChanged(PaymentStatusChangedEvent event) {
        if (event.status() != PaymentStatus.AUTHORIZED) {
            return;
        }
        log.info("[Receipt] event=IssueRequested paymentId={} status={} occurredAt={}",
                event.paymentId(), event.status(), event.occurredAt());
        receiptService.issueForPayment(event.paymentId());
        log.info("[Receipt] event=IssuedFromPayment paymentId={}", event.paymentId());
    }
}
