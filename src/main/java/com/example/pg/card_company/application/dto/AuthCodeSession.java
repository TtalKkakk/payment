package com.example.pg.card_company.application.dto;

public record AuthCodeSession(
        String merchantId,
        String billingKeyToken,
        String cardCompanyCode,
        String cardBrand,
        String cardNumberMasked,
        String expiryMasked
) {
}
