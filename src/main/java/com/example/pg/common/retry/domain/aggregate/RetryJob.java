package com.example.pg.common.retry.domain.aggregate;

import com.example.pg.common.retry.domain.enumerate.RetryJobStatus;
import com.example.pg.common.retry.domain.converter.AttemptCountConverter;
import com.example.pg.common.retry.domain.converter.IdempotencyKeyConverter;
import com.example.pg.common.retry.domain.converter.JobTypeConverter;
import com.example.pg.common.retry.domain.converter.MaxAttemptsConverter;
import com.example.pg.common.retry.domain.vo.AttemptCount;
import com.example.pg.common.retry.domain.vo.IdempotencyKey;
import com.example.pg.common.retry.domain.vo.JobType;
import com.example.pg.common.retry.domain.vo.MaxAttempts;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "retry_jobs",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_retry_jobs_type_key", columnNames = {"job_type", "idempotency_key"})
        },
        indexes = {
                @Index(name = "ix_retry_jobs_due", columnList = "status,next_run_at,expires_at"),
                @Index(name = "ix_retry_jobs_lock", columnList = "status,locked_until")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RetryJob {

    @Getter
    @Id
    @Column(length = 36)
    private String id;

    @Getter
    @Column(name = "job_type", nullable = false, length = 100)
    @Convert(converter = JobTypeConverter.class)
    private JobType jobType;

    @Getter
    @Column(name = "idempotency_key", nullable = false, length = 200)
    @Convert(converter = IdempotencyKeyConverter.class)
    private IdempotencyKey idempotencyKey;

    @Getter
    @Lob
    @Column(name = "payload_json", nullable = false, columnDefinition = "LONGTEXT")
    private String payloadJson;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RetryJobStatus status;

    @Getter
    @Column(name = "attempt_count", nullable = false)
    @Convert(converter = AttemptCountConverter.class)
    private AttemptCount attemptCount;

    @Getter
    @Column(name = "max_attempts", nullable = false)
    @Convert(converter = MaxAttemptsConverter.class)
    private MaxAttempts maxAttempts;

    @Getter
    @Column(name = "next_run_at", nullable = false)
    private LocalDateTime nextRunAt;

    @Getter
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Getter
    @Column(name = "last_error_message", length = 2000)
    private String lastErrorMessage;

    @Getter
    @Column(name = "last_failed_at")
    private LocalDateTime lastFailedAt;

    @Getter
    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Getter
    @Column(name = "locked_by", length = 100)
    private String lockedBy;

    @Getter
    @Column(name = "lock_token", length = 36)
    private String lockToken;

    @Getter
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Getter
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private long version;

    public static RetryJob newPending(
            String jobType,
            String idempotencyKey,
            String payloadJson,
            int maxAttempts,
            LocalDateTime nextRunAt,
            LocalDateTime expiresAt
    ) {
        Objects.requireNonNull(payloadJson, "payloadJson");
        Objects.requireNonNull(nextRunAt, "nextRunAt");
        Objects.requireNonNull(expiresAt, "expiresAt");
        JobType jt = JobType.of(jobType);
        IdempotencyKey ik = IdempotencyKey.of(idempotencyKey);
        MaxAttempts ma = MaxAttempts.of(maxAttempts);

        RetryJob job = new RetryJob();
        job.id = UUID.randomUUID().toString();
        job.jobType = jt;
        job.idempotencyKey = ik;
        job.payloadJson = payloadJson;
        job.status = RetryJobStatus.PENDING;
        job.attemptCount = AttemptCount.of(0);
        job.maxAttempts = ma;
        job.nextRunAt = nextRunAt;
        job.expiresAt = expiresAt;
        job.createdAt = LocalDateTime.now();
        job.updatedAt = job.createdAt;
        return job;
    }

    public void overwritePayloadAndSchedule(LocalDateTime nextRunAt, LocalDateTime expiresAt, int maxAttempts, String payloadJson) {
        if (status == RetryJobStatus.SUCCEEDED) {
            return;
        }
        this.payloadJson = payloadJson;
        this.maxAttempts = MaxAttempts.of(Math.max(this.maxAttempts.value(), maxAttempts));
        this.nextRunAt = nextRunAt;
        this.expiresAt = expiresAt;
        this.updatedAt = LocalDateTime.now();
        if (status == RetryJobStatus.DEAD) {
            this.status = RetryJobStatus.PENDING;
            this.attemptCount = AttemptCount.of(0);
            this.lastErrorMessage = null;
            this.lastFailedAt = null;
        }
    }

    public boolean isExpired(LocalDateTime now) {
        return !now.isBefore(expiresAt);
    }

    public boolean canAttempt(LocalDateTime now) {
        return status == RetryJobStatus.PENDING && !isExpired(now) && !now.isBefore(nextRunAt);
    }

    public boolean lock(String workerId, Duration lease, LocalDateTime now) {
        if (status != RetryJobStatus.PENDING) {
            return false;
        }
        if (lockedUntil != null && now.isBefore(lockedUntil)) {
            return false;
        }
        this.status = RetryJobStatus.RUNNING;
        this.lockedBy = workerId;
        this.lockedUntil = now.plus(lease);
        this.lockToken = UUID.randomUUID().toString();
        this.updatedAt = LocalDateTime.now();
        return true;
    }

    public void succeed() {
        this.status = RetryJobStatus.SUCCEEDED;
        this.lockedBy = null;
        this.lockedUntil = null;
        this.lockToken = null;
        this.updatedAt = LocalDateTime.now();
    }

    public void failAndReschedule(String errorMessage, LocalDateTime nextRunAt) {
        this.attemptCount = this.attemptCount.increment();
        this.lastErrorMessage = truncate(errorMessage, 2000);
        this.lastFailedAt = LocalDateTime.now();
        this.status = RetryJobStatus.PENDING;
        this.nextRunAt = nextRunAt;
        this.lockedBy = null;
        this.lockedUntil = null;
        this.lockToken = null;
        this.updatedAt = LocalDateTime.now();
    }

    public void dead(String errorMessage) {
        this.attemptCount = this.attemptCount.increment();
        this.lastErrorMessage = truncate(errorMessage, 2000);
        this.lastFailedAt = LocalDateTime.now();
        this.status = RetryJobStatus.DEAD;
        this.lockedBy = null;
        this.lockedUntil = null;
        this.lockToken = null;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isExhaustedAfterFailure() {
        return attemptCount.value() + 1 >= maxAttempts.value();
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return null;
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen);
    }
}

