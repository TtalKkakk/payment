package com.example.pg.card_company.application.dto;

import com.example.pg.card_company.presentation.dto.BillingKeyTokenResponse;

import java.time.Instant;

/**
 * 빌링키 등록 완료 시 가맹점 서버로 POST하는 웹훅 본문 스키마.
 * 결제 웹훅과 동일하게 raw JSON 문자열로 HMAC 서명한다.
 */
public record BillingKeyRegisteredWebhookDto(
        String eventType,
        String merchantId,
        String cardCompanyCode,
        String billingKeyToken,
        String cardBrand,
        String cardNumberMasked,
        String expiryMasked,
        Instant occurredAt
) {
    public static final String EVENT_TYPE = "BILLING_KEY_REGISTERED";

    public static BillingKeyRegisteredWebhookDto of(
            String merchantId,
            String cardCompanyCode,
            BillingKeyTokenResponse token
    ) {
        return new BillingKeyRegisteredWebhookDto(
                EVENT_TYPE,
                merchantId,
                cardCompanyCode,
                token.billingKeyToken(),
                token.cardBrand(),
                token.cardNumberMasked(),
                token.expiryMasked(),
                Instant.now()
        );
    }
}
