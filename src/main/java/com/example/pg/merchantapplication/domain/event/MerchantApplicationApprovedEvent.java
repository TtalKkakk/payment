package com.example.pg.merchantapplication.domain.event;

import java.time.LocalDateTime;

/**
 * 가맹점 신청 승인 도메인 이벤트
 */
public record MerchantApplicationApprovedEvent(
        String applicationId,
        String name,
        LocalDateTime occurredAt
) {
    public static MerchantApplicationApprovedEvent from(String applicationId, String name) {
        return new MerchantApplicationApprovedEvent(applicationId, name, LocalDateTime.now());
    }
}
