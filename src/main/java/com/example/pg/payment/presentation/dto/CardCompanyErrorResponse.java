package com.example.pg.payment.presentation.dto;

/**
 * 카드사 API 오류 응답.
 */
public record CardCompanyErrorResponse(
        String code,
        String message
) {}
