package com.example.pg.payment.application;

import com.example.pg.common.retry.card_company.CardCompanyPaymentRetryExecutor;
import com.example.pg.card_company.util.CardCompanyPortRegistry;
import com.example.pg.card_company.presentation.CardCompanyConnect;
import com.example.pg.payment.domain.aggregate.Payment;
import com.example.pg.payment.domain.enumerate.PaymentFailureCategory;
import com.example.pg.payment.domain.enumerate.PaymentStatus;
import com.example.pg.payment.domain.event.PaymentStatusChangedEvent;
import com.example.pg.payment.domain.vo.PaymentId;
import com.example.pg.payment.infrastructure.lock.PaymentProcessDistributedLock;
import com.example.pg.payment.infrastructure.persistence.PaymentRepository;
import com.example.pg.payment.presentation.dto.PaymentRefundResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 카드사에 환불(취소) 요청을 비동기로 수행하고 결과를 결제 상태에 반영한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentRefundProcessor {

    private final PaymentRepository paymentRepository;
    private final CardCompanyPortRegistry portRegistry;
    private final ApplicationEventPublisher eventPublisher;
    private final PaymentProcessDistributedLock processLock;
    private final CardCompanyPaymentRetryExecutor cardCompanyPaymentRetryExecutor;

    @Async
    @Transactional
    public void processRefund(String paymentIdValue) {
        processLock.runWithRefundLock(paymentIdValue, () -> {
            PaymentId paymentId = PaymentId.from(paymentIdValue);
            paymentRepository.load(paymentId).ifPresent(payment -> runRefund(paymentIdValue, payment));
        });
    }

    private void runRefund(String paymentIdValue, Payment payment) {
        CardCompanyConnect port = portRegistry.getPortOrThrow(payment.getCardCompany().getCode());

        try {
            PaymentRefundResponse result = cardCompanyPaymentRetryExecutor.refund(port, payment.getId());
            onRefundCallReturned(paymentIdValue, result);
        } catch (Exception e) {
            onRefundCallThrew(paymentIdValue, e);
        }
    }

    private void onRefundCallReturned(String paymentIdValue, PaymentRefundResponse result) {
        if (result.success()) {
            applyCancelSuccess(paymentIdValue);
            return;
        }
        applyCancelBusinessFailure(paymentIdValue, result);
    }

    private void applyCancelSuccess(String paymentIdValue) {
        int updated = paymentRepository.markCanceled(paymentIdValue);
        if (updated != 1) {
            log.warn("[Payment] skip cancelSuccess due to status race paymentId={}", paymentIdValue);
            return;
        }
        eventPublisher.publishEvent(PaymentStatusChangedEvent.from(paymentIdValue, PaymentStatus.CANCELED));
        log.info("[Payment] event=Canceled paymentId={}", paymentIdValue);
    }

    private void applyCancelBusinessFailure(String paymentIdValue, PaymentRefundResponse result) {
        int updated = paymentRepository.markCancelFailed(
                paymentIdValue,
                PaymentFailureCategory.BUSINESS,
                PaymentFailureSnapshotSupport.normalizeCode(result.resultCode()),
                PaymentFailureSnapshotSupport.truncateMessage(result.message()),
                LocalDateTime.now()
        );
        if (updated != 1) {
            log.warn("[Payment] skip cancelFail due to status race paymentId={}", paymentIdValue);
            return;
        }
        eventPublisher.publishEvent(PaymentStatusChangedEvent.from(paymentIdValue, PaymentStatus.CANCEL_FAILED));
        log.warn("[Payment] event=RefundRequestFailed paymentId={} status=CANCEL_FAILED resultCode={} message={}",
                paymentIdValue, result.resultCode(), result.message());
    }

    private void onRefundCallThrew(String paymentIdValue, Exception e) {
        int updated = paymentRepository.markCancelFailed(
                paymentIdValue,
                PaymentFailureCategory.TECHNICAL,
                "PG_TECHNICAL",
                PaymentFailureSnapshotSupport.truncateMessage(PaymentFailureSnapshotSupport.technicalExceptionSummary(e)),
                LocalDateTime.now()
        );
        if (updated != 1) {
            log.warn("[Payment] skip cancelFail(technical) due to status race paymentId={}", paymentIdValue, e);
            return;
        }
        eventPublisher.publishEvent(PaymentStatusChangedEvent.from(paymentIdValue, PaymentStatus.CANCEL_FAILED));
        log.warn("[Payment] event=RefundFailed(technical) paymentId={}", paymentIdValue, e);
    }
}
