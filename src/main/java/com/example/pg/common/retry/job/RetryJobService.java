package com.example.pg.common.retry.job;

import com.example.pg.common.retry.domain.aggregate.RetryJob;
import com.example.pg.common.retry.domain.enumerate.RetryJobStatus;
import com.example.pg.common.retry.domain.vo.IdempotencyKey;
import com.example.pg.common.retry.domain.vo.JobType;
import com.example.pg.common.retry.infrastructure.persistence.RetryJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RetryJobService {

    private final RetryJobRepository retryJobRepository;

    @Transactional
    public RetryJob upsertPending(
            String jobType,
            String idempotencyKey,
            String payloadJson,
            int maxAttempts,
            Duration initialDelay,
            Duration ttl
    ) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRunAt = now.plus(initialDelay);
        LocalDateTime expiresAt = now.plus(ttl);

        JobType jt = JobType.of(jobType);
        IdempotencyKey ik = IdempotencyKey.of(idempotencyKey);

        return retryJobRepository.findByJobTypeAndIdempotencyKey(jt, ik)
                .map(existing -> {
                    existing.overwritePayloadAndSchedule(nextRunAt, expiresAt, maxAttempts, payloadJson);
                    return existing;
                })
                .orElseGet(() -> {
                    RetryJob created = RetryJob.newPending(jobType, idempotencyKey, payloadJson, maxAttempts, nextRunAt, expiresAt);
                    try {
                        return retryJobRepository.save(created);
                    } catch (DataIntegrityViolationException e) {
                        RetryJob raced = retryJobRepository.findByJobTypeAndIdempotencyKey(jt, ik)
                                .orElseThrow(() -> e);
                        raced.overwritePayloadAndSchedule(nextRunAt, expiresAt, maxAttempts, payloadJson);
                        return raced;
                    }
                });
    }

    @Transactional(readOnly = true)
    public boolean isSucceeded(String jobType, String idempotencyKey) {
        JobType jt = JobType.of(jobType);
        IdempotencyKey ik = IdempotencyKey.of(idempotencyKey);
        return retryJobRepository.findByJobTypeAndIdempotencyKey(jt, ik)
                .map(j -> j.getStatus() == RetryJobStatus.SUCCEEDED)
                .orElse(false);
    }
}

