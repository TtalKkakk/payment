package com.example.pg.payment.domain.event;

import java.time.LocalDateTime;

/**
 * 결제 취소(환불) 시작 도메인 이벤트.
 * 트랜잭션 커밋 후 비동기 카드사 환불 요청을 트리거한다.
 */
public record CancellationStartedEvent(
        String paymentId,
        LocalDateTime occurredAt
) {
    public static CancellationStartedEvent from(String paymentId) {
        return new CancellationStartedEvent(paymentId, LocalDateTime.now());
    }
}
