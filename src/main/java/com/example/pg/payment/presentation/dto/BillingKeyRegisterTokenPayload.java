package com.example.pg.payment.presentation.dto;

/**
 * 카드 등록 페이지 접근용 서명 토큰 payload.
 * - token은 브라우저에 노출되어도 괜찮지만, 위조 방지를 위해 apiSecret으로 서명(HMAC)되어야 한다.
 */
public record BillingKeyRegisterTokenPayload(
        String apiKey,
        String returnUrl,
        long iat,
        long exp,
        String nonce,
        String purpose
) {
}

