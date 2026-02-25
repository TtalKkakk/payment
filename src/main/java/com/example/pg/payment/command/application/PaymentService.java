package com.example.pg.payment.command.application;

import com.example.pg.payment.command.application.port.RefundPort;
import com.example.pg.payment.command.domain.aggregate.Payment;
import com.example.pg.payment.command.domain.enumerate.PaymentStatus;
import com.example.pg.payment.command.domain.event.PaymentCreatedEvent;
import com.example.pg.payment.command.domain.event.PaymentStatusChangedEvent;
import com.example.pg.payment.command.domain.repository.PaymentCommandRepository;
import com.example.pg.payment.command.domain.vo.PaymentId;
import com.example.pg.exception.BusinessException;
import com.example.pg.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentCommandRepository paymentCommandRepository;
    private final PaymentAuthorizationProcessor paymentAuthorizationProcessor;
    private final RefundPort refundPort;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public PaymentId createPayment(String merchantId, long amount,
                                  String merchantOrderId, String orderName,
                                  String customerEmail, String customerName, String callbackUrl) {
        validateAmount(amount);
        PaymentId paymentId = PaymentId.generate();
        Payment payment = new Payment(
                paymentId, merchantId, amount,
                merchantOrderId, orderName, customerEmail, customerName, callbackUrl
        );
        paymentCommandRepository.save(payment);

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

        Payment payment = paymentCommandRepository.load(PaymentId.from(paymentIdValue))
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND, paymentIdValue));

        payment.startAuthorization();

        // 카드사에 빌링키로 청구 요청 (비동기 시뮬레이션)
        paymentAuthorizationProcessor.processAuthorization(paymentIdValue, billingKey);
    }

    /**
     * 결제 취소(환불)를 요청한다.
     * 카드사에 환불 요청 후 승인되면 status를 CANCELED로 변경하고 웹훅을 발송한다.
     * 해당 가맹점의 결제만 취소할 수 있다.
     */
    @Transactional
    public void cancelPayment(String merchantId, String paymentIdValue) {
        Payment payment = paymentCommandRepository.load(PaymentId.from(paymentIdValue))
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
