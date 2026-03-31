package com.example.pg.common.retry.job;

import com.example.pg.common.retry.domain.aggregate.RetryJob;
import com.example.pg.common.retry.domain.enumerate.RetryJobStatus;
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

        return retryJobRepository.findByJobTypeAndIdempotencyKey(jobType, idempotencyKey)
                .map(existing -> {
                    // DEAD를 살린다고? -> DEAD를 살렸을 때 서비스 부하가 발생할 가능성은?
                    existing.overwritePayloadAndSchedule(nextRunAt, expiresAt, maxAttempts, payloadJson);
                    return existing;
                })
                .orElseGet(() -> {
                    // vo로 인증하는걸로 고치기
                    RetryJob created = RetryJob.newPending(jobType, idempotencyKey, payloadJson, maxAttempts, nextRunAt, expiresAt);
                    try {
                        return retryJobRepository.save(created);
                    } catch (DataIntegrityViolationException e) {
                        // 동시 생성 경합 시 재조회 후 갱신
                        RetryJob raced = retryJobRepository.findByJobTypeAndIdempotencyKey(jobType, idempotencyKey)
                                .orElseThrow(() -> e);
                        raced.overwritePayloadAndSchedule(nextRunAt, expiresAt, maxAttempts, payloadJson);
                        return raced;
                    }
                });
    }

    @Transactional(readOnly = true)
    public boolean isSucceeded(String jobType, String idempotencyKey) {
        return retryJobRepository.findByJobTypeAndIdempotencyKey(jobType, idempotencyKey)
                .map(j -> j.getStatus() == RetryJobStatus.SUCCEEDED)
                .orElse(false);
    }
}

