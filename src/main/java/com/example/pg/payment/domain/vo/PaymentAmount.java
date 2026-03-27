package com.example.pg.payment.domain.vo;

public record PaymentAmount(long value) {
    public PaymentAmount {
        if (value <= 0) {
            throw new IllegalArgumentException("payment amount must be greater than 0");
        }
    }
}
