package com.example.pg.card_company.presentation.dto;

/**
 * billingKey 교환 응답.
 */
public record BillingKeyExchangeResponse(
        String billingKeyToken,
        String cardCompanyCode,
        String cardBrand,
        String cardNumberMasked,
        String expiryMasked
) {
}

