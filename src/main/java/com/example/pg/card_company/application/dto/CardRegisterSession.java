package com.example.pg.card_company.application.dto;

public record CardRegisterSession(
        String cardCompanyCode,
        String returnUrl,
        String merchantId
) {
}