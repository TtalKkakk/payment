package com.example.pg.common.retry.infrastructure.persistence;

import com.example.pg.common.retry.domain.aggregate.RetryJob;
import com.example.pg.common.retry.domain.enumerate.RetryJobStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RetryJobRepository extends JpaRepository<RetryJob, String> {

    Optional<RetryJob> findByJobTypeAndIdempotencyKey(
            com.example.pg.common.retry.domain.vo.JobType jobType,
            com.example.pg.common.retry.domain.vo.IdempotencyKey idempotencyKey
    );

    @Query("""
            select j from RetryJob j
            where j.status = :status
              and j.nextRunAt <= :now
              and j.expiresAt > :now
            order by j.nextRunAt asc, j.createdAt asc
            """)
    List<RetryJob> findDue(
            @Param("status") RetryJobStatus status,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    @Modifying
    @Query("""
            update RetryJob j
               set j.status = :runningStatus,
                   j.lockedBy = :workerId,
                   j.lockedUntil = :lockedUntil,
                   j.lockToken = :lockToken,
                   j.updatedAt = :now
             where j.id = :id
               and j.status = :pendingStatus
               and (j.lockedUntil is null or j.lockedUntil < :now)
               and j.nextRunAt <= :now
               and j.expiresAt > :now
            """)
    int tryClaim(
            @Param("id") String id,
            @Param("pendingStatus") RetryJobStatus pendingStatus,
            @Param("runningStatus") RetryJobStatus runningStatus,
            @Param("workerId") String workerId,
            @Param("lockedUntil") LocalDateTime lockedUntil,
            @Param("lockToken") String lockToken,
            @Param("now") LocalDateTime now
    );

    /**
     * findDue/tryClaim에서 제외된 만료 PENDING 잡을 DEAD로 정리한다. 한 틱당 최대 {@code limit}건.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE retry_jobs
               SET status = 'DEAD',
                   attempt_count = attempt_count + 1,
                   last_error_message = :message,
                   last_failed_at = :now,
                   locked_by = NULL,
                   locked_until = NULL,
                   lock_token = NULL,
                   updated_at = :now,
                   version = version + 1
             WHERE status = 'PENDING'
               AND expires_at <= :now
             LIMIT :limit
            """, nativeQuery = true)
    int markExpiredPendingAsDead(
            @Param("now") LocalDateTime now,
            @Param("message") String message,
            @Param("limit") int limit
    );

    /**
     * 워커 크래시 등으로 lease가 지난 RUNNING(또는 locked_until이 비어 있는 비정상 RUNNING)을 PENDING으로 되돌린다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE retry_jobs
               SET status = 'PENDING',
                   locked_by = NULL,
                   locked_until = NULL,
                   lock_token = NULL,
                   last_error_message = :message,
                   last_failed_at = :now,
                   updated_at = :now,
                   version = version + 1
             WHERE status = 'RUNNING'
               AND (locked_until IS NULL OR locked_until < :now)
             LIMIT :limit
            """, nativeQuery = true)
    int reclaimStaleRunningJobs(
            @Param("now") LocalDateTime now,
            @Param("message") String message,
            @Param("limit") int limit
    );
}

