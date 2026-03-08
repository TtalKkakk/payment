package com.example.pg.cardcompany.domain.event;

import java.time.LocalDateTime;

/**
 * 카드사에서 빌링키 발급이 완료되었을 때 발행.
 * 확장 시: 로그/감사, 정산 반영, PG 알림 등 구독 가능. 빌링키 값은 포함하지 않는다.
 */
public record BillingKeyIssuedEvent(
        String merchantId,
        String cardToken,
        LocalDateTime occurredAt
) {
    public static BillingKeyIssuedEvent from(String merchantId, String cardToken) {
        return new BillingKeyIssuedEvent(merchantId, cardToken, LocalDateTime.now());
    }
}
