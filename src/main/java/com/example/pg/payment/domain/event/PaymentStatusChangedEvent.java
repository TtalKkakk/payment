package com.example.pg.payment.domain.event;

import com.example.pg.payment.domain.enumerate.PaymentStatus;

import java.time.LocalDateTime;

/**
 * 결제 상태 변경 도메인 이벤트.
 * 승인 완료/실패 시 발행되어 웹훅 발송 등 후속 처리를 트리거한다.
 */
public record PaymentStatusChangedEvent(
        String paymentId,
        PaymentStatus status,
        LocalDateTime occurredAt
) {
    public static PaymentStatusChangedEvent from(String paymentId, PaymentStatus status) {
        return new PaymentStatusChangedEvent(paymentId, status, LocalDateTime.now());
    }
}
