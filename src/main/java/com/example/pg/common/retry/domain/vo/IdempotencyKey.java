package com.example.pg.common.retry.domain.vo;

import java.util.Objects;

public record IdempotencyKey(String value) {
    public IdempotencyKey {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey must not be blank");
        }
        if (value.length() > 200) {
            throw new IllegalArgumentException("idempotencyKey must be <= 200 chars");
        }
    }

    public static IdempotencyKey of(String value) {
        return new IdempotencyKey(value);
    }
}

