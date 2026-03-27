package com.example.pg.payment.domain.vo;

public record PaymentCustomerName(String value) {
    public PaymentCustomerName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("payment customerName is required");
        }
        if (value.length() > 100) {
            throw new IllegalArgumentException("payment customerName length must be <= 100");
        }
    }
}
