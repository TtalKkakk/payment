package com.example.pg.cardcompany.infrastructure.adapter.dto;

/**
 * 카드사 등록 세션 생성 요청 (POST /card-registration-session).
 */
public record SessionRequest(
        String returnUrl
) {}
