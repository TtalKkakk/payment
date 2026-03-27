package com.example.pg.payment.domain.vo;

public record PaymentCallbackUrl(String value) {
    public PaymentCallbackUrl {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("payment callbackUrl is required");
        }
        if (value.length() > 500) {
            throw new IllegalArgumentException("payment callbackUrl length must be <= 500");
        }
    }
}
