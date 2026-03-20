package com.example.pg.payment.presentation.dto;

public record BillingKeyTokenResponse(
        String billingKeyToken,
        String cardBrand,
        String cardNumberMasked,
        String expiryMasked
) {
}
