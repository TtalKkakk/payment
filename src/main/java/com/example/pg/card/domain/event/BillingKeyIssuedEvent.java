package com.example.pg.card.domain.event;

import java.time.LocalDateTime;

/**
 * 빌링키 발급 완료 도메인 이벤트.
 * authCode → 빌링키 교환 성공 후 발행. billingKey·authCode는 포함하지 않는다.
 */
public record BillingKeyIssuedEvent(
        String merchantId,
        String ownerId,
        String maskedNumber,
        LocalDateTime occurredAt
) {
    public static BillingKeyIssuedEvent from(String merchantId, String ownerId, String maskedNumber) {
        return new BillingKeyIssuedEvent(merchantId, ownerId, maskedNumber, LocalDateTime.now());
    }
}
