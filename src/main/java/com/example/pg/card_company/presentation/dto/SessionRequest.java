package com.example.pg.card_company.presentation.dto;

/**
 * 카드사 등록 세션 생성 요청 (POST /card-registration-session).
 */
public record SessionRequest(
        String returnUrl
) {}
