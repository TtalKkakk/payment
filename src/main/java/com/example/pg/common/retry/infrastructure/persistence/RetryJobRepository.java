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
}

