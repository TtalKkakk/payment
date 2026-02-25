package com.example.pg.merchant.presentation.dto;

/**
 * 관리자 가맹점 목록 응답 (한 건)
 */
public record AdminMerchantListItemResponse(
        String id,
        String name,
        String apiKey,
        String status
) {}
