package com.example.pg.payment.domain.vo;

public record PaymentMerchantOrderId(String value) {
    public PaymentMerchantOrderId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("payment merchantOrderId is required");
        }
        if (value.length() > 100) {
            throw new IllegalArgumentException("payment merchantOrderId length must be <= 100");
        }
    }
}
