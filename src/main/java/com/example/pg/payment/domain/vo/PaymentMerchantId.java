package com.example.pg.payment.domain.vo;

public record PaymentMerchantId(String value) {
    public PaymentMerchantId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("payment merchantId is required");
        }
        if (value.length() > 36) {
            throw new IllegalArgumentException("payment merchantId length must be <= 36");
        }
    }
}
