package com.example.pg.cardcompany.infrastructure.adapter.dto;

/**
 * 카드사 빌링키 발급 응답 (가이드 3.3).
 */
public record BillingKeyResponse(
        String billingKeyToken
) {}
