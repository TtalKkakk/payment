package com.example.pg.payment.application;

import com.example.pg.common.exception.BusinessException;
import com.example.pg.common.exception.ErrorCode;
import com.example.pg.payment.application.retry.PaymentRetryJobEnqueuer;
import com.example.pg.payment.domain.vo.PaymentId;
import com.example.pg.payment.infrastructure.persistence.PaymentRepository;
import com.example.pg.payment.presentation.dto.CreatePaymentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 결제 생성(Tx1) + 승인 시작(Tx2) 오케스트레이션.
 * <p>
 * {@link PaymentService} 내부에서 {@code this.createPayment()} / {@code this.startAuthorization()}를 호출하면
 * Spring AOP 프록시를 타지 않아 {@code @Transactional}이 적용되지 않는(self-invocation) 문제가 생길 수 있다.
 * 이 클래스는 별도 빈에서 {@link PaymentService}를 주입해 호출하여 프록시를 경유하게 한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentOrchestratorService {

    private final PaymentService paymentService;
    private final PaymentRetryJobEnqueuer paymentRetryJobEnqueuer;

    public PaymentId createPaymentAndStartAuthorization(
            String merchantId,
            CreatePaymentRequest request,
            String idempotencyKey
    ) {
        PaymentId paymentId;
        try {
            String hash = PaymentIdempotencyFingerprint.sha256Hex(request);
            paymentId = paymentService.createPaymentWithIdempotency(merchantId, request, idempotencyKey, hash);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PAYMENT_CREATION_FAILED);
        }

        try {
            paymentService.startAuthorization(paymentId.getValue(), request.billingKey());
        } catch (Exception e) {
            handleCompensationFailure(paymentId, e);
            throw new BusinessException(ErrorCode.AUTHORIZATION_START_FAILED, paymentId.getValue());
        }
        return paymentId;
    }

    private void handleCompensationFailure(PaymentId paymentId, Exception original) {
        try {
            paymentService.compensateCreationFailure(paymentId.getValue());
        } catch (Exception ce) {
            log.error("[Payment] compensation failed paymentId={}", paymentId.getValue(), ce);
            original.addSuppressed(ce);
            paymentRetryJobEnqueuer.enqueueCompensateCreationFailure(paymentId.getValue());
        }
    }
}
