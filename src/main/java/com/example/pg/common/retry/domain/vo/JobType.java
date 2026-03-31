package com.example.pg.common.retry.domain.vo;

import java.util.Objects;

public record JobType(String value) {
    public JobType {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("jobType must not be blank");
        }
        if (value.length() > 100) {
            throw new IllegalArgumentException("jobType must be <= 100 chars");
        }
    }

    public static JobType of(String value) {
        return new JobType(value);
    }
}

