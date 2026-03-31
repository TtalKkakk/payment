package com.example.pg.common.retry.domain.vo;

public record AttemptCount(int value) {
    public AttemptCount {
        if (value < 0) {
            throw new IllegalArgumentException("attemptCount must be >= 0");
        }
    }

    public static AttemptCount of(int value) {
        return new AttemptCount(value);
    }

    public AttemptCount increment() {
        return new AttemptCount(value + 1);
    }
}

