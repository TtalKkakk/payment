package com.example.pg.payment.domain.event;

import java.time.LocalDateTime;

/**
 * 결제 승인 시작 도메인 이벤트.
 * 트랜잭션 커밋 후 비동기 승인 처리를 트리거하기 위해 사용한다.
 */
public record AuthorizationStartedEvent(
        String paymentId,
        String billingKey,
        LocalDateTime occurredAt
) {
    public static AuthorizationStartedEvent from(String paymentId, String billingKey) {
        return new AuthorizationStartedEvent(paymentId, billingKey, LocalDateTime.now());
    }
}
