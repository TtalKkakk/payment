package com.example.pg.payment.application;

import com.example.pg.common.exception.BusinessException;
import com.example.pg.common.exception.ErrorCode;
import com.example.pg.payment.domain.vo.PaymentId;
import com.example.pg.payment.presentation.dto.CreatePaymentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
    private final com.example.pg.payment.application.retry.PaymentRetryJobEnqueuer paymentRetryJobEnqueuer;

    /**
     * 결제하기 단일 API: 트랜잭션 1(결제 생성 READY) + 트랜잭션 2(승인 요청 AUTHORIZING).
     * Tx1 실패 시 PAYMENT_CREATION_FAILED, Tx2 실패 시 보상(ABORTED) 후 AUTHORIZATION_START_FAILED.
     */
    public PaymentId createPaymentAndStartAuthorization(String merchantId, CreatePaymentRequest request) {
        PaymentId paymentId;
        try {
            paymentId = paymentService.createPayment(
                    merchantId,
                    request.amount(),
                    request.merchantOrderId(),
                    request.orderName(),
                    request.customerEmail(),
                    request.customerName(),
                    request.callbackUrl(),
                    request.cardCompanyCode()
            );
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
