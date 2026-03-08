package com.example.pg.cardcompany.domain.event;

import java.time.LocalDateTime;

/**
 * 카드사에서 특정 가맹점의 빌링키가 일괄 차단되었을 때 발행.
 * 확장 시: 감사 로그, PG 동기화 등 구독 가능.
 */
public record BillingKeysRevokedEvent(
        String merchantId,
        LocalDateTime occurredAt
) {
    public static BillingKeysRevokedEvent from(String merchantId) {
        return new BillingKeysRevokedEvent(merchantId, LocalDateTime.now());
    }
}
