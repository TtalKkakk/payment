package com.example.pg.idempotency.domain.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 가맹점·멱등 키 단위 기록. 동일 키에 동일 요청 지문이면 같은 리소스 ID로 수렴한다.
 */
@Entity
@Table(
        name = "idempotency_keys",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_idem_merchant_key",
                columnNames = {"merchant_id", "idempotency_key"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class IdempotencyKey {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "merchant_id", nullable = false, length = 100)
    private String merchantId;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "resource_id", length = 36)
    private String resourceId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public static IdempotencyKey createPending(String merchantId, String idempotencyKey, String requestHash) {
        IdempotencyKey row = new IdempotencyKey();
        row.id = UUID.randomUUID().toString();
        row.merchantId = merchantId;
        row.idempotencyKey = idempotencyKey;
        row.requestHash = requestHash;
        row.resourceId = null;
        LocalDateTime now = LocalDateTime.now();
        row.createdAt = now;
        row.updatedAt = now;
        return row;
    }

    public void assignResource(String resourceId) {
        this.resourceId = resourceId;
        this.updatedAt = LocalDateTime.now();
    }
}
