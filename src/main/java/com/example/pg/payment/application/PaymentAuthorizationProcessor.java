package com.example.pg.payment.application;

import com.example.pg.card_company.util.CardCompanyPortRegistry;
import com.example.pg.card_company.presentation.CardCompanyConnect;
import com.example.pg.payment.presentation.dto.PaymentApproveResponse;
import com.example.pg.card_company.domain.aggergate.CardCompany;
import com.example.pg.payment.domain.aggregate.Payment;
import com.example.pg.payment.domain.enumerate.PaymentFailureCategory;
import com.example.pg.payment.domain.enumerate.PaymentStatus;
import com.example.pg.payment.domain.event.PaymentStatusChangedEvent;
import com.example.pg.payment.domain.vo.PaymentId;
import com.example.pg.payment.infrastructure.lock.PaymentProcessDistributedLock;
import com.example.pg.payment.infrastructure.persistence.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentAuthorizationProcessor {

    private final PaymentRepository paymentRepository;
    private final CardCompanyPortRegistry portRegistry;
    private final ApplicationEventPublisher eventPublisher;
    private final PaymentProcessDistributedLock processLock;

    /**
     * 카드사에 결제 승인 요청(POST /api/pg/payments/approve)을 보내고 결과를 반영한다.
     * 결제에 연결된 카드사(cardCompanyCode)에 맞는 포트를 레지스트리에서 꺼내 사용한다.
     */
    @Async
    @Transactional
    public void processAuthorization(String paymentIdValue, String billingKey) {
        processLock.runWithAuthorizeLock(paymentIdValue, () -> {
            PaymentId paymentId = PaymentId.from(paymentIdValue);
            paymentRepository.load(paymentId).ifPresent(payment ->
                    runAuthorization(paymentIdValue, billingKey, payment));
        });
    }

    private void runAuthorization(String paymentIdValue, String billingKey, Payment payment) {
        CardCompany cardCompany = payment.getCardCompany();
        CardCompanyConnect port = portRegistry.getPortOrThrow(cardCompany.getCode());

        try {
            PaymentApproveResponse result = port.approve(
                    payment.getId(),
                    payment.getAmount(),
                    billingKey
            );
            onApproveCallReturned(paymentIdValue, result);
        } catch (Exception e) {
            onApproveCallThrew(paymentIdValue, e);
        }
    }

    private void onApproveCallReturned(String paymentIdValue, PaymentApproveResponse result) {
        if (result.success()) {
            applyAuthorizeSuccess(paymentIdValue, result);
            return;
        }
        applyAuthorizeBusinessFailure(paymentIdValue, result);
    }

    private void applyAuthorizeSuccess(String paymentIdValue, PaymentApproveResponse result) {
        LocalDateTime approvedAt = result.approvedAt();
        int updated = paymentRepository.markAuthorized(
                paymentIdValue,
                result.approvalNumber(),
                result.transactionId(),
                approvedAt != null ? approvedAt : LocalDateTime.now()
        );
        if (updated != 1) {
            log.warn("[Payment] skip authorizeSuccess due to status race paymentId={}", paymentIdValue);
            return;
        }
        eventPublisher.publishEvent(PaymentStatusChangedEvent.from(paymentIdValue, PaymentStatus.AUTHORIZED));
        log.info("[Payment] event=Authorized paymentId={} approvalNumber={} transactionId={}",
                paymentIdValue, result.approvalNumber(), result.transactionId());
    }

    private void applyAuthorizeBusinessFailure(String paymentIdValue, PaymentApproveResponse result) {
        int updated = paymentRepository.markAuthorizeFailed(
                paymentIdValue,
                PaymentFailureCategory.BUSINESS,
                PaymentFailureSnapshotSupport.normalizeCode(result.resultCode()),
                PaymentFailureSnapshotSupport.truncateMessage(result.message()),
                LocalDateTime.now()
        );
        if (updated != 1) {
            log.warn("[Payment] skip authorizeFail due to status race paymentId={}", paymentIdValue);
            return;
        }
        eventPublisher.publishEvent(PaymentStatusChangedEvent.from(paymentIdValue, PaymentStatus.AUTHORIZE_FAILED));
        log.info("[Payment] event=Failed paymentId={} resultCode={} message={}",
                paymentIdValue, result.resultCode(), result.message());
    }

    private void onApproveCallThrew(String paymentIdValue, Exception e) {
        int updated = paymentRepository.markAuthorizeFailed(
                paymentIdValue,
                PaymentFailureCategory.TECHNICAL,
                "PG_TECHNICAL",
                PaymentFailureSnapshotSupport.truncateMessage(PaymentFailureSnapshotSupport.technicalExceptionSummary(e)),
                LocalDateTime.now()
        );
        if (updated != 1) {
            log.warn("[Payment] skip authorizeFail(technical) due to status race paymentId={}", paymentIdValue, e);
            return;
        }
        eventPublisher.publishEvent(PaymentStatusChangedEvent.from(paymentIdValue, PaymentStatus.AUTHORIZE_FAILED));
        log.warn("[Payment] event=Failed(technical) paymentId={}", paymentIdValue, e);
    }
}
