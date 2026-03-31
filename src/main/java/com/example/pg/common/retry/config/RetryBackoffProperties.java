package com.example.pg.common.retry.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "app.retry.backoff")
public class RetryBackoffProperties {

    private Policy defaultPolicy = new Policy();
    private Map<String, Policy> byJobType = new HashMap<>();

    public Policy getDefaultPolicy() {
        return defaultPolicy;
    }

    public void setDefaultPolicy(Policy defaultPolicy) {
        this.defaultPolicy = defaultPolicy;
    }

    public Map<String, Policy> getByJobType() {
        return byJobType;
    }

    public void setByJobType(Map<String, Policy> byJobType) {
        this.byJobType = byJobType;
    }

    public static class Policy {
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

        public long getInitialDelayMs() {
            return initialDelayMs;
        }

        public void setInitialDelayMs(long initialDelayMs) {
            this.initialDelayMs = initialDelayMs;
        }

        public double getMultiplier() {
            return multiplier;
        }

        public void setMultiplier(double multiplier) {
            this.multiplier = multiplier;
        }

        public long getMaxDelayMs() {
            return maxDelayMs;
        }

        public void setMaxDelayMs(long maxDelayMs) {
            this.maxDelayMs = maxDelayMs;
        }

        public double getJitterRatio() {
            return jitterRatio;
        }

        public void setJitterRatio(double jitterRatio) {
            this.jitterRatio = jitterRatio;
        }
    }
}

