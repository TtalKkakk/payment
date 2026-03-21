package com.example.pg.card.domain.event;

import java.time.LocalDateTime;

/**
 * 카드 등록 완료 도메인 이벤트.
 * registerCard 성공 후 발행. authCode·cardToken 등 민감 정보는 포함하지 않는다.
 */
public record CardRegisteredEvent(
        String merchantId,
        String ownerId,
        String maskedNumber,
        LocalDateTime occurredAt
) {
    public static CardRegisteredEvent from(String merchantId, String ownerId, String maskedNumber) {
        return new CardRegisteredEvent(merchantId, ownerId, maskedNumber, LocalDateTime.now());
    }
}
