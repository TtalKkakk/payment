package com.example.pg.merchant.domain.event;

import java.time.LocalDateTime;

/**
 * 가맹점 정지 해제 도메인 이벤트 (SUSPENDED → ACTIVE)
 */
public record MerchantActivatedEvent(
        String merchantId,
        String name,
        LocalDateTime occurredAt
) {
    public static MerchantActivatedEvent from(String merchantId, String name) {
        return new MerchantActivatedEvent(merchantId, name, LocalDateTime.now());
    }
}
