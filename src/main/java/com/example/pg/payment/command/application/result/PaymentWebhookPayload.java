package com.example.pg.payment.command.application.result;

import com.example.pg.payment.domain.aggregate.Payment;

import java.time.LocalDateTime;

/**
 * 웹훅 요청 본문.
 * 가맹점은 이 payload를 수신하여 결제 결과를 처리한다.
 */
public record PaymentWebhookPayload(
        String paymentId,
        String status,
        String merchantId,
        String merchantOrderId,
        long amount,
        String orderName,
        LocalDateTime occurredAt
) {
    public static PaymentWebhookPayload from(Payment payment) {
        return new PaymentWebhookPayload(
                payment.getId(),
                payment.getStatus().name(),
                payment.getMerchantId(),
                payment.getMerchantOrderId(),
                payment.getAmount(),
                payment.getOrderName(),
                LocalDateTime.now()
        );
    }
}
