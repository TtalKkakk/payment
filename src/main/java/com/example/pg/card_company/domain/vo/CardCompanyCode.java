package com.example.pg.card_company.domain.vo;

public record CardCompanyCode(String value) {
    public CardCompanyCode {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("cardCompany code is required");
        }
        if (value.length() > 50) {
            throw new IllegalArgumentException("cardCompany code length must be <= 50");
        }
    }
}

