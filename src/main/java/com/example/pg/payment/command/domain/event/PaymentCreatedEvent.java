package com.example.pg.payment.command.domain.event;

import java.time.LocalDateTime;

/**
 * 결제 생성 완료 도메인 이벤트
 */
public record PaymentCreatedEvent(
        String paymentId,
        String merchantId,
        long amount,
        LocalDateTime occurredAt
) {
    public static PaymentCreatedEvent from(String paymentId, String merchantId, long amount) {
        return new PaymentCreatedEvent(paymentId, merchantId, amount, LocalDateTime.now());
    }
}
