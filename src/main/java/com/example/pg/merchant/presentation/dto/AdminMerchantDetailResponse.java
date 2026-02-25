package com.example.pg.merchant.presentation.dto;

/**
 * 관리자 가맹점 상세 응답 (apiSecret 포함)
 */
public record AdminMerchantDetailResponse(
        String id,
        String name,
        String apiKey,
        String apiSecret,
        String status
) {}
