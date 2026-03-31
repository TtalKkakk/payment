package com.example.pg.common.retry.worker;

import com.example.pg.common.exception.NonRetryableJobException;
import com.example.pg.common.retry.backoff.BackoffPolicy;
import com.example.pg.common.retry.backoff.JobTypeBackoffPolicyResolver;
import com.example.pg.common.retry.config.RetryWorkerProperties;
import com.example.pg.common.retry.domain.aggregate.RetryJob;
import com.example.pg.common.retry.domain.enumerate.RetryJobStatus;
import com.example.pg.common.retry.handler.RetryJobHandler;
import com.example.pg.common.retry.infrastructure.persistence.RetryJobRepository;
import com.example.pg.common.retry.handler.RetryJobHandlerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.retry", name = "enabled", havingValue = "true")
public class RetryJobWorker {

    private final RetryWorkerProperties props;
    private final RetryJobRepository retryJobRepository;
    private final RetryJobHandlerRegistry registry;
    private final JobTypeBackoffPolicyResolver backoffPolicyResolver;
    private final String workerId = "retry-worker-" + UUID.randomUUID();

    @Scheduled(fixedDelayString = "${app.retry.poll-interval-ms:60000}")
    public void pollOnce() {
        LocalDateTime now = LocalDateTime.now();
        List<RetryJob> due = retryJobRepository.findDue(RetryJobStatus.PENDING, now, PageRequest.of(0, props.batchSize()));
        for (RetryJob job : due) {
            tryProcessOne(job.getId());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void tryProcessOne(String jobId) {
        LocalDateTime now = LocalDateTime.now();
        String lockToken = UUID.randomUUID().toString();
        LocalDateTime lockedUntil = now.plus(Duration.ofMillis(props.lockLeaseMs()));

        int claimed = retryJobRepository.tryClaim(
                jobId,
                RetryJobStatus.PENDING,
                RetryJobStatus.RUNNING,
                workerId,
                lockedUntil,
                lockToken,
                now
        );
        if (claimed != 1) {
            return;
        }

        RetryJob job = retryJobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return;
        }

        if (job.isExpired(now)) {
            job.dead("Expired before execution.");
            return;
        }

        try {
            RetryJobHandler handler = registry.getRequired(job.getJobType().value());
            handler.handle(job);
            job.succeed();
            log.info("[RetryJob] succeeded id={} type={} key={}", job.getId(), job.getJobType().value(), job.getIdempotencyKey().value());
        } catch (NonRetryableJobException e) {
            job.dead(messageOf(e));
            log.warn("[RetryJob] dead(non-retryable) id={} type={} key={} msg={}", job.getId(), job.getJobType().value(), job.getIdempotencyKey().value(), e.getMessage());
        } catch (Exception e) {
            boolean exhausted = job.isExhaustedAfterFailure();
            retryMethod(e, exhausted, job);
        }
    }

    private void retryMethod(Exception e, boolean exhausted, RetryJob job) {
        if (exhausted || job.isExpired(LocalDateTime.now())) {
            job.dead(messageOf(e));
            log.error("[RetryJob] dead(exhausted/expired) id={} type={} key={}", job.getId(), job.getJobType().value(), job.getIdempotencyKey().value(), e);
        } else {
            int nextAttemptNumber = job.getAttemptCount().value() + 1;
            BackoffPolicy backoffPolicy = backoffPolicyResolver.resolve(job.getJobType().value());
            LocalDateTime nextRunAt = backoffPolicy.nextRunAt(nextAttemptNumber, LocalDateTime.now());
            job.failAndReschedule(messageOf(e), nextRunAt);
            log.warn("[RetryJob] rescheduled id={} type={} key={} nextRunAt={} attempts={}/{}",
                    job.getId(), job.getJobType().value(), job.getIdempotencyKey().value(), nextRunAt, job.getAttemptCount().value(), job.getMaxAttempts().value());
        }
    }

    private static String messageOf(Throwable t) {
        if (t == null) return null;
        String msg = t.getMessage();
        if (msg != null && !msg.isBlank()) return msg;
        return t.getClass().getSimpleName();
    }
}

