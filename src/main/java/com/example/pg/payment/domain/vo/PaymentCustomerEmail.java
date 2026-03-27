package com.example.pg.payment.domain.vo;

public record PaymentCustomerEmail(String value) {
    public PaymentCustomerEmail {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("payment customerEmail is required");
        }
        if (value.length() > 200) {
            throw new IllegalArgumentException("payment customerEmail length must be <= 200");
        }
    }
}
