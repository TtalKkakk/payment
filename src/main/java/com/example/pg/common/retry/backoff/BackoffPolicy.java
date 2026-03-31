package com.example.pg.common.retry.backoff;

import java.time.LocalDateTime;

public interface BackoffPolicy {
    LocalDateTime nextRunAt(int nextAttemptNumber, LocalDateTime now);
}

