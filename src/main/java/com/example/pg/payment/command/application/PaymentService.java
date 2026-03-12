package com.example.pg.payment.command.application;

import com.example.pg.payment.command.application.port.RefundPort;
import com.example.pg.payment.domain.aggregate.Payment;
import com.example.pg.payment.domain.enumerate.PaymentStatus;
import com.example.pg.payment.domain.event.PaymentCreatedEvent;
import com.example.pg.payment.domain.event.PaymentStatusChangedEvent;
import com.example.pg.payment.domain.vo.PaymentId;
import com.example.pg.exception.BusinessException;
import com.example.pg.exception.ErrorCode;
import com.example.pg.payment.infrastructure.persistence.CardCompanyRepository;
import com.example.pg.payment.infrastructure.persistence.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final CardCompanyRepository cardCompanyRepository;
    private final PaymentAuthorizationProcessor paymentAuthorizationProcessor;
    private final RefundPort refundPort;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 트랜잭션 1: 결제 객체 생성 → 상태 READY.
     * 트랜잭션 실패 시 가맹점에게 결제 생성 실패 및 사유 전달.
     */

    @Transactional
    public PaymentId createPayment(String merchantId, long amount,
                                  String merchantOrderId, String orderName,
                                  String customerEmail, String customerName, String callbackUrl,
                                  String cardCompanyCode) {
        validateAmount(amount);
        PaymentId paymentId = PaymentId.generate();
        var cardCompany = (cardCompanyCode != null && !cardCompanyCode.isBlank())
                ? cardCompanyRepository.findByCode(cardCompanyCode).orElse(null)
                : null;
        Payment payment = new Payment(
                paymentId, merchantId, amount,
                merchantOrderId, orderName, customerEmail, customerName, callbackUrl,
                cardCompany
        );
        paymentRepository.save(payment);

        eventPublisher.publishEvent(PaymentCreatedEvent.from(
                payment.getId(), payment.getMerchantId(), payment.getAmount()));

        return paymentId;
    }

    /**
     * 빌링키로 결제 승인을 시작한다. 상태를 AUTHORIZING으로 변경하고,
     * 비동기 프로세스가 카드사에 빌링키 청구 요청을 수행하도록 위임한다.
     */
    @Transactional
    public void startAuthorization(String paymentIdValue, String billingKey) {
        if (billingKey == null || billingKey.isBlank()) {
            throw new BusinessException(ErrorCode.BILLING_KEY_REQUIRED);
        }

        Payment payment = paymentRepository.load(PaymentId.from(paymentIdValue))
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND, paymentIdValue));

        payment.startAuthorization();

        // 카드사에 빌링키로 청구 요청 (비동기 시뮬레이션)
        paymentAuthorizationProcessor.processAuthorization(paymentIdValue, billingKey);
    }

    /** Tx2(승인 시작) 실패 시 보상: READY 결제를 ABORTED로 무효화. 별도 트랜잭션으로 실행 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void compensateCreationFailure(String paymentIdValue) {
        paymentRepository.findByIdAndStatus(paymentIdValue, PaymentStatus.READY)
                .ifPresent(Payment::markAsAborted);
    }

    /**
     * 결제하기 단일 API: 트랜잭션 1(결제 생성 READY) + 트랜잭션 2(승인 요청 AUTHORIZING).
     * Tx1 실패 시 PAYMENT_CREATION_FAILED, Tx2 실패 시 보상(ABORTED) 후 AUTHORIZATION_START_FAILED.
     */
    public PaymentId createPaymentAndStartAuthorization(String merchantId, long amount,
                                                        String merchantOrderId, String orderName,
                                                        String customerEmail, String customerName,
                                                        String callbackUrl, String billingKey,
                                                        String cardCompanyCode) {
        validateAmount(amount);
        if (billingKey == null || billingKey.isBlank()) {
            throw new BusinessException(ErrorCode.BILLING_KEY_REQUIRED);
        }

        PaymentId paymentId;
        try {
            paymentId = createPayment(merchantId, amount, merchantOrderId, orderName,
                    customerEmail, customerName, callbackUrl, cardCompanyCode);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PAYMENT_CREATION_FAILED);
        }

        try {
            startAuthorization(paymentId.getValue(), billingKey);
        } catch (Exception e) {
            compensateCreationFailure(paymentId.getValue());
            throw new BusinessException(ErrorCode.AUTHORIZATION_START_FAILED, paymentId.getValue());
        }

        return paymentId;
    }

    /**
     * 결제 취소(환불)를 요청한다.
     * 카드사에 환불 요청 후 승인되면 status를 CANCELED로 변경하고 웹훅을 발송한다.
     * 해당 가맹점의 결제만 취소할 수 있다.
     */
    @Transactional
    public void cancelPayment(String merchantId, String paymentIdValue) {
        Payment payment = paymentRepository.load(PaymentId.from(paymentIdValue))
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND, paymentIdValue));

        if (!payment.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ErrorCode.PAYMENT_INVALID_STATUS, "해당 결제에 대한 취소 권한이 없습니다.");
        }

        if (!refundPort.requestRefund(payment.getId(), payment.getAmount())) {
            throw new BusinessException(ErrorCode.PAYMENT_INVALID_STATUS, "환불 요청 실패");
        }

        payment.cancel();

        eventPublisher.publishEvent(PaymentStatusChangedEvent.from(payment.getId(), PaymentStatus.CANCELED));
    }

    private void validateAmount(long amount) {
        if (amount <= 0) {
            throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_INVALID, amount);
        }
    }
}
