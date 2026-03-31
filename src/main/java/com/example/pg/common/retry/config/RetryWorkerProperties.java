package com.example.pg.common.retry.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.retry")
public record RetryWorkerProperties(
        boolean enabled,
        long pollIntervalMs,
        int batchSize,
        long lockLeaseMs
) {
}

