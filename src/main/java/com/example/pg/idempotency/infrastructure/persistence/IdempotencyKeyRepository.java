package com.example.pg.idempotency.infrastructure.persistence;

import com.example.pg.idempotency.domain.aggregate.IdempotencyKey;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, String> {

    boolean existsByMerchantIdAndIdempotencyKey(String merchantId, String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT k FROM IdempotencyKey k
             WHERE k.merchantId = :merchantId
               AND k.idempotencyKey = :idempotencyKey
            """)
    Optional<IdempotencyKey> findByMerchantIdAndIdempotencyKeyForUpdate(
            @Param("merchantId") String merchantId,
            @Param("idempotencyKey") String idempotencyKey
    );
}
