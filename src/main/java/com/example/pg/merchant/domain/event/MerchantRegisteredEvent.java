package com.example.pg.merchant.domain.event;

import java.time.LocalDateTime;

/**
 * 가맹점 등록 완료 도메인 이벤트
 */
public record MerchantRegisteredEvent(
        String merchantId,
        String name,
        LocalDateTime occurredAt
) {
    public static MerchantRegisteredEvent from(String merchantId, String name) {
        return new MerchantRegisteredEvent(merchantId, name, LocalDateTime.now());
    }
}
