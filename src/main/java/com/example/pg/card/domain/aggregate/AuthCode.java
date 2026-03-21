package com.example.pg.card.domain.aggregate;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 1회용 인증 코드. 가맹점이 authCode로 빌링키 교환 시 사용한다.
 * Redis에 저장되며, 만료 후 또는 1회 사용 후 폐기된다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthCode {

    private String code;
    private String cardToken;
    private String merchantId;
    private String ownerId;
    private String maskedNumber;
    private LocalDateTime expiresAt;
    private boolean used;
    private LocalDateTime createdAt;

    public AuthCode(String code, String cardToken, String merchantId, String ownerId,
                    String maskedNumber, LocalDateTime expiresAt) {
        this.code = code;
        this.cardToken = cardToken;
        this.merchantId = merchantId;
        this.ownerId = ownerId;
        this.maskedNumber = maskedNumber;
        this.expiresAt = expiresAt;
        this.used = false;
        this.createdAt = LocalDateTime.now();
    }

    public void markAsUsed() {
        this.used = true;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isUsable() {
        return !used && !isExpired();
    }
}
