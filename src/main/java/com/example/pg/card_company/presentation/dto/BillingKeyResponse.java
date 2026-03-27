package com.example.pg.card_company.presentation.dto;

/**
 * 카드사 빌링키 발급 응답 (가이드 3.3).
 */
public record BillingKeyResponse(
        String billingKeyToken,
        String cardBrand,
        String cardNumberMasked,
        String expiryMasked
) {}
