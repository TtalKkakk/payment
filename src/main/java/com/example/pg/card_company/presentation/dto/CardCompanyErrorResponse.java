package com.example.pg.card_company.presentation.dto;

/**
 * 카드사 API 오류 응답.
 */
public record CardCompanyErrorResponse(
        String code,
        String message
) {}
