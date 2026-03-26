package com.example.pg.card_company.domain.vo;

public record BaseUrl(String value) {
    public BaseUrl {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("cardCompany baseUrl is required");
        }
    }
    public String normalize() {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
