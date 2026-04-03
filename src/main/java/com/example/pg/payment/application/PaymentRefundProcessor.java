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
import com.example.pg.payment.application.retry.PaymentRetryJobEnqueuer;
import com.example.pg.payment.presentation.dto.PaymentRefundResponse;
import com.example.pg.common.exception.NonRetryableJobException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

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
    private final PaymentRetryJobEnqueuer paymentRetryJobEnqueuer;

    @Async
    @Transactional
    public void processRefund(String paymentIdValue) {
        processLock.runWithRefundLock(paymentIdValue, () -> {
            PaymentId paymentId = PaymentId.from(paymentIdValue);
            paymentRepository.load(paymentId).ifPresent(payment -> runRefund(paymentIdValue, payment));
        });
    }

    /**
     * 장기 재시도(DB RetryJob) 워커에서 동기 실행. CANCEL_FAILED(기술) → CANCELLING CAS 후 카드사 환불을 다시 호출한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processRefundRetryFromJob(String paymentIdValue) {
        processLock.runWithRefundLock(paymentIdValue, () -> runRefundRetryLocked(paymentIdValue));
    }

    private void runRefundRetryLocked(String paymentIdValue) {
        PaymentId paymentId = PaymentId.from(paymentIdValue);
        Payment initial = loadPaymentOrThrow(paymentId, paymentIdValue);

        if (initial.getStatus() == PaymentStatus.CANCELED) {
            log.info("[Payment] refund retry skip already CANCELED paymentId={}", paymentIdValue);
            return;
        }

        if (!prepareCancellingForRefundRetry(initial, paymentIdValue)) {
            return;
        }

        Payment latest = loadPaymentOrThrow(paymentId, paymentIdValue);
        applyRefundRetryAfterReload(latest, paymentIdValue);
    }

    /** CANCEL_FAILED → CANCELLING CAS(또는 이미 CANCELLING). CAS 실패 시 재조회 후 후속 처리까지 하면 false. */
    private boolean prepareCancellingForRefundRetry(Payment initial, String paymentIdValue) {
        if (initial.getStatus() == PaymentStatus.CANCEL_FAILED) {
            if (initial.getLastFailureCategory() == PaymentFailureCategory.BUSINESS) {
                throw new NonRetryableJobException(
                        "Refund retry not applicable for BUSINESS failure paymentId=" + paymentIdValue
                );
            }
            int updated = paymentRepository.transitionStatusFromSet(
                    paymentIdValue,
                    Set.of(PaymentStatus.CANCEL_FAILED),
                    PaymentStatus.CANCELLING
            );
            if (updated != 1) {
                Payment reconciled = loadPaymentOrThrow(PaymentId.from(paymentIdValue), paymentIdValue);
                applyRefundRetryAfterReload(reconciled, paymentIdValue);
                return false;
            }
            return true;
        }
        if (initial.getStatus() != PaymentStatus.CANCELLING) {
            throw new NonRetryableJobException(
                    "Refund retry unexpected status=" + initial.getStatus() + " paymentId=" + paymentIdValue
            );
        }
        return true;
    }

    /** 재로딩 시점에 허용되는 상태(CANCELED / CANCELLING / 경합 CANCEL_FAILED)만 처리. */
    private void applyRefundRetryAfterReload(Payment payment, String paymentIdValue) {
        switch (payment.getStatus()) {
            case CANCELED -> log.info("[Payment] refund retry skip already CANCELED paymentId={}", paymentIdValue);
            case CANCELLING -> runRefund(paymentIdValue, payment);
            case CANCEL_FAILED -> throw new IllegalStateException(
                    "Concurrent refund retry; reschedule paymentId=" + paymentIdValue
            );
            default -> throw new NonRetryableJobException(
                    "Refund retry unexpected status=" + payment.getStatus() + " paymentId=" + paymentIdValue
            );
        }
    }

    private Payment loadPaymentOrThrow(PaymentId paymentId, String paymentIdValue) {
        return paymentRepository.load(paymentId)
                .orElseThrow(() -> new NonRetryableJobException("Payment not found: " + paymentIdValue));
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
        paymentRetryJobEnqueuer.enqueueRefundRetryAfterTechnicalFailure(paymentIdValue);
    }
}
