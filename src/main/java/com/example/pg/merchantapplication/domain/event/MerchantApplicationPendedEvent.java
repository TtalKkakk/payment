package com.example.pg.merchantapplication.domain.event;

import java.time.LocalDateTime;

/**
 * 가맹점 신청 접수 도메인 이벤트
 */
public record MerchantApplicationPendedEvent(
        String applicationId,
        String name,
        String businessNumber,
        String contactEmail,
        LocalDateTime occurredAt
) {
    public static MerchantApplicationPendedEvent from(String applicationId, String name,
                                                      String businessNumber, String contactEmail) {
        return new MerchantApplicationPendedEvent(
                applicationId, name, businessNumber, contactEmail, LocalDateTime.now());
    }
}
