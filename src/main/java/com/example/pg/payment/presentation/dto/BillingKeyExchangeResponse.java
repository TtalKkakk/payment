package com.example.pg.payment.presentation.dto;

/**
 * billingKey 교환 응답.
 */
public record BillingKeyExchangeResponse(
        String billingKeyToken,
        String cardCompanyCode
) {
}

