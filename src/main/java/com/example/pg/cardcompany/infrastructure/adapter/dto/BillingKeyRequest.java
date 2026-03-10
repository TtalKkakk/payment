package com.example.pg.cardcompany.infrastructure.adapter.dto;

/**
 * 카드사 빌링키 발급 요청 (POST /billing-keys).
 */
public record BillingKeyRequest(
        String authCode
) {}
