package com.example.pg.payment.application;

import com.example.pg.payment.domain.aggregate.Payment;
import com.example.pg.payment.domain.enumerate.PaymentFailureCategory;
import com.example.pg.payment.domain.enumerate.PaymentStatus;
import com.example.pg.payment.domain.event.PaymentStatusChangedEvent;
import com.example.pg.payment.domain.vo.PaymentId;
import com.example.pg.payment.infrastructure.persistence.PaymentRepository;
import com.example.pg.payment.presentation.dto.PaymentApproveResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 결제 승인 흐름에서 DB 접근이 필요한 구간만 트랜잭션으로 묶는 전용 퍼시스터.
 *
 * <p>배경:
 * {@link PaymentAuthorizationProcessor#processAuthorization}은 @Async 스레드에서 실행되며,
 * 카드사 HTTP 호출(~10s)을 포함한다. @Transactional을 processAuthorization 전체에 걸면
 * HTTP 대기 시간 동안 HikariCP 커넥션이 점유되어 커넥션 풀이 고갈된다.
 *
 * <p>이 클래스는 실제로 커넥션이 필요한 구간(load, write)만 각각 짧은 트랜잭션으로 처리하며,
 * HTTP 호출 중에는 커넥션을 반납한다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentAuthorizationPersister {

    private final PaymentRepository paymentRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 결제를 조회한다. cardCompany는 JOIN FETCH로 함께 로드하여
     * 트랜잭션 종료 후에도 LazyInitializationException 없이 접근 가능하다.
     */
    @Transactional(readOnly = true)
    public Optional<Payment> loadPaymentWithCardCompany(PaymentId paymentId) {
        return paymentRepository.loadWithCardCompany(paymentId);
    }

    @Transactional
    public void applyAuthorizeSuccess(String paymentIdValue, PaymentApproveResponse result) {
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

    @Transactional
    public void applyAuthorizeBusinessFailure(String paymentIdValue, PaymentApproveResponse result) {
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

    @Transactional
    public void applyAuthorizeTechnicalFailure(String paymentIdValue, Exception e) {
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
