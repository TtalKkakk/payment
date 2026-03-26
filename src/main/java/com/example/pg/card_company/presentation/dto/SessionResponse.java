package com.example.pg.card_company.presentation.dto;

/**
 * 카드사 등록 세션 생성 응답 (가이드 3.1).
 */
public record SessionResponse(
        String token,
        String registrationUrl
) {}
