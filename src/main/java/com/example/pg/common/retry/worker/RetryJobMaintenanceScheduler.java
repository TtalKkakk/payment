package com.example.pg.common.retry.worker;

import com.example.pg.common.retry.infrastructure.persistence.RetryJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * RetryJob 정리: 만료된 PENDING을 DEAD로, lease가 끝난 RUNNING을 PENDING으로 되돌린다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.retry", name = "enabled", havingValue = "true")
public class RetryJobMaintenanceScheduler {

    private static final String MSG_EXPIRED_PENDING = "Expired (pending sweep).";
    private static final String MSG_RECLAIM_RUNNING = "Reclaimed after RUNNING lease expired.";

    private final RetryJobRepository retryJobRepository;

    @Scheduled(fixedDelayString = "${app.retry.housekeeping-interval-ms:120000}")
    @Transactional
    public void runHousekeeping() {
        LocalDateTime now = LocalDateTime.now();
        int batchLimit = 500;
        int expired = retryJobRepository.markExpiredPendingAsDead(now, MSG_EXPIRED_PENDING, batchLimit);
        int reclaimed = retryJobRepository.reclaimStaleRunningJobs(now, MSG_RECLAIM_RUNNING, batchLimit);
        if (expired > 0 || reclaimed > 0) {
            log.info("[RetryJob] housekeeping expiredPendingDead={} reclaimedRunningToPending={}", expired, reclaimed);
        }
    }
}
