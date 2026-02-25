package com.example.pg.merchantapplication.domain.event;

import java.time.LocalDateTime;

/**
 * 가맹점 신청 거절 도메인 이벤트
 */
public record MerchantApplicationRejectedEvent(
        String applicationId,
        String name,
        String rejectReason,
        LocalDateTime occurredAt
) {
    public static MerchantApplicationRejectedEvent from(String applicationId, String name, String rejectReason) {
        return new MerchantApplicationRejectedEvent(
                applicationId, name, rejectReason != null ? rejectReason : "", LocalDateTime.now());
    }
}
