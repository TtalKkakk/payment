package com.example.pg.payment.domain.vo;

public record PaymentOrderName(String value) {
    public PaymentOrderName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("payment orderName is required");
        }
        if (value.length() > 200) {
            throw new IllegalArgumentException("payment orderName length must be <= 200");
        }
    }
}
