package com.example.pg.merchant.domain.event;

import java.time.LocalDateTime;

/**
 * 가맹점 API 정지 도메인 이벤트 (ACTIVE → SUSPENDED)
 */
public record MerchantSuspendedEvent(
        String merchantId,
        String name,
        LocalDateTime occurredAt
) {
    public static MerchantSuspendedEvent from(String merchantId, String name) {
        return new MerchantSuspendedEvent(merchantId, name, LocalDateTime.now());
    }
}
