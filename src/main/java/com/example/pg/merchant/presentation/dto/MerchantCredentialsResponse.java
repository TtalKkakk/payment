package com.example.pg.merchant.presentation.dto;

/**
 * 사업자번호·비밀번호 인증 후 조회한 API 키 정보.
 * 승인(APPROVED)된 신청에만 발급된다.
 */
public record MerchantCredentialsResponse(
        String apiKey,
        String apiSecret
) {
}
