package com.example.pg.card_company.presentation.dto;

public record BillingKeyTokenResponse(
        String billingKeyToken,
        String cardBrand,
        String cardNumberMasked,
        String expiryMasked
) {
}
