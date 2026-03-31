package com.example.pg.common.retry.domain.vo;

public record MaxAttempts(int value) {
    public MaxAttempts {
        if (value < 1) {
            throw new IllegalArgumentException("maxAttempts must be >= 1");
        }
    }

    public static MaxAttempts of(int value) {
        return new MaxAttempts(value);
    }
}

