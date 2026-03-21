package com.example.pg.merchant.domain.event;

import java.time.LocalDateTime;

/**
 * 가맹점 API Secret 재발급 도메인 이벤트
 * (Secret 값은 보안상 이벤트에 포함하지 않음)
 */
public record MerchantSecretRegeneratedEvent(
        String merchantId,
        LocalDateTime occurredAt
) {
    public static MerchantSecretRegeneratedEvent from(String merchantId) {
        return new MerchantSecretRegeneratedEvent(merchantId, LocalDateTime.now());
    }
}
