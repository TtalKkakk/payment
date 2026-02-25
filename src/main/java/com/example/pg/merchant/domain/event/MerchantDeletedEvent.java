package com.example.pg.merchant.domain.event;

import java.time.LocalDateTime;

/**
 * 가맹점 탈퇴(soft delete) 도메인 이벤트.
 * applicationId가 있으면 해당 신청을 SUBSCRIPTION_ENDED로 전이하는 리스너에서 사용한다.
 */
public record MerchantDeletedEvent(
        String merchantId,
        String name,
        String applicationId,
        LocalDateTime occurredAt
) {
    public static MerchantDeletedEvent from(String merchantId, String name, String applicationId) {
        return new MerchantDeletedEvent(merchantId, name, applicationId, LocalDateTime.now());
    }
}
