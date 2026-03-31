package com.example.pg.common.retry.backoff.impl;

import com.example.pg.common.retry.backoff.BackoffPolicy;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

/**
 * nextAttemptNumber는 1부터 시작(첫 실패 후 다음 시도 스케줄링).
 */
public class ExponentialJitterBackoffPolicy implements BackoffPolicy {

    private final Duration initialDelay;
    private final double multiplier;
    private final Duration maxDelay;
    private final double jitterRatio;

    public ExponentialJitterBackoffPolicy(Duration initialDelay, double multiplier, Duration maxDelay, double jitterRatio) {
        if (initialDelay.isNegative() || initialDelay.isZero()) throw new IllegalArgumentException("initialDelay must be > 0");
        if (multiplier < 1.0) throw new IllegalArgumentException("multiplier must be >= 1.0");
        if (maxDelay.isNegative() || maxDelay.isZero()) throw new IllegalArgumentException("maxDelay must be > 0");
        if (jitterRatio < 0.0 || jitterRatio > 1.0) throw new IllegalArgumentException("jitterRatio must be between 0 and 1");
        this.initialDelay = initialDelay;
        this.multiplier = multiplier;
        this.maxDelay = maxDelay;
        this.jitterRatio = jitterRatio;
    }

    @Override
    public LocalDateTime nextRunAt(int nextAttemptNumber, LocalDateTime now) {
        Duration base = calcBaseDelay(nextAttemptNumber);
        Duration jittered = applyJitter(base);
        return now.plus(jittered);
    }

    private Duration calcBaseDelay(int nextAttemptNumber) {
        double pow = Math.pow(multiplier, Math.max(0, nextAttemptNumber - 1));
        long millis = (long) Math.min(maxDelay.toMillis(), Math.round(initialDelay.toMillis() * pow));
        return Duration.ofMillis(Math.max(1L, millis));
    }

    private Duration applyJitter(Duration base) {
        if (jitterRatio == 0.0) return base;
        long baseMs = base.toMillis();
        long delta = (long) Math.floor(baseMs * jitterRatio);
        long min = Math.max(1L, baseMs - delta);
        long max = baseMs + delta;
        long chosen = ThreadLocalRandom.current().nextLong(min, max + 1);
        return Duration.ofMillis(chosen);
    }
}

