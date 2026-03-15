package com.example.pg.merchantapplication.domain.event;

import java.time.LocalDateTime;

/**
 * 가맹점 신청 구독 종료 도메인 이벤트 (Merchant 탈퇴 등으로 포트를 통해 호출된 경우)
 */
public record MerchantApplicationSubscriptionEndedEvent(
        String applicationId,
        String name,
        String previousStatus,
        LocalDateTime occurredAt
) {
    public static MerchantApplicationSubscriptionEndedEvent from(String applicationId, String name, String previousStatus) {
        return new MerchantApplicationSubscriptionEndedEvent(
                applicationId, name, previousStatus != null ? previousStatus : "", LocalDateTime.now());
    }
}
