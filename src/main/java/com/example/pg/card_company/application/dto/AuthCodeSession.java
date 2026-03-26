package com.example.pg.card_company.application.dto;

public record AuthCodeSession(
        String merchantId,
        String billingKeyToken,
        String cardCompanyCode,
        String cardBrand,
        String cardNumberMasked,
        String expiryMasked
) {
    public AuthCodeSession {
        if (merchantId == null || merchantId.isBlank()) {
            throw new IllegalArgumentException("merchantId must not be null or blank");
        }
        if (billingKeyToken == null || billingKeyToken.isBlank()) {
            throw new IllegalArgumentException("billingKeyToken must not be null or blank");
        }
        if (cardCompanyCode == null || cardCompanyCode.isBlank()) {
            throw new IllegalArgumentException("cardCompanyCode must not be null or blank");
        }
        if (cardNumberMasked == null || cardNumberMasked.isBlank()) {
            throw new IllegalArgumentException("cardNumberMasked must not be null or blank");
        }
        if (expiryMasked == null || expiryMasked.isBlank()) {
            throw new IllegalArgumentException("expiryMasked must not be null or blank");
        }
        cardBrand = cardBrand == null ? "" : cardBrand;
    }
}
