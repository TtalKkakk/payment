package com.example.pg.merchantapplication.domain.event;

import java.time.LocalDateTime;

/**
 * 가맹점 신청 취소 도메인 이벤트 (사업자번호+비밀번호로 사용자가 취소한 경우)
 */
public record MerchantApplicationCancelledEvent(
        String applicationId,
        String name,
        String businessNumber,
        LocalDateTime occurredAt
) {
    public static MerchantApplicationCancelledEvent from(String applicationId, String name, String businessNumber) {
        return new MerchantApplicationCancelledEvent(
                applicationId, name, businessNumber, LocalDateTime.now());
    }
}
