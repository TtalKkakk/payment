package com.example.pg.common.retry.config;

import lombok.Getter;

@Getter
public class Policy {
    /**
     * Exponential backoff base delay.
     */
    private long initialDelayMs = 10_000;
    private double multiplier = 2.0;
    /**
     * Maximum delay cap.
     */
    private long maxDelayMs = 6L * 60 * 60 * 1000;
    /**
     * 0.2 = ±20%
     */
    private double jitterRatio = 0.2;

    public void setInitialDelayMs(long initialDelayMs) {
        this.initialDelayMs = initialDelayMs;
    }

    public void setMultiplier(double multiplier) {
        this.multiplier = multiplier;
    }

    public void setMaxDelayMs(long maxDelayMs) {
        this.maxDelayMs = maxDelayMs;
    }

    public void setJitterRatio(double jitterRatio) {
        this.jitterRatio = jitterRatio;
    }

}
