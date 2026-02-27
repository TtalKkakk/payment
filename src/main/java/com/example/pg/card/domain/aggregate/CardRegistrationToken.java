package com.example.pg.card.domain.aggregate;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 카드 등록 폼 접근용 1회용 토큰.
 * Redis에 저장되며, 카드 등록 완료 시 파기된다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CardRegistrationToken {

    private String token;
    private String merchantId;
    private String ownerId;
    private String returnUrl;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    public CardRegistrationToken(String token, String merchantId, String ownerId, String returnUrl) {
        this.token = token;
        this.merchantId = merchantId;
        this.ownerId = ownerId;
        this.returnUrl = returnUrl;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = null;
    }

    /** Redis 등 저장소에서 복원할 때 사용 (createdAt, expiresAt 포함) */
    public CardRegistrationToken(String token, String merchantId, String ownerId, String returnUrl,
                                 LocalDateTime createdAt, LocalDateTime expiresAt) {
        this.token = token;
        this.merchantId = merchantId;
        this.ownerId = ownerId;
        this.returnUrl = returnUrl;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public void updateReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }
}
