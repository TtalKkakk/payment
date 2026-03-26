package com.example.pg.card_company.domain.vo;

public record CardCompanyName(String value) {
    public CardCompanyName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("cardCompany name is required");
        }
        if (value.length() > 100) {
            throw new IllegalArgumentException("cardCompany name length must be <= 100");
        }
    }
}

