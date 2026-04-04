package com.example.pg.idempotency.application;

import com.example.pg.common.exception.BusinessException;
import com.example.pg.common.exception.ErrorCode;
import com.example.pg.idempotency.domain.aggregate.IdempotencyKey;
import com.example.pg.idempotency.infrastructure.persistence.IdempotencyKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * 멱등 키 행 예약·락·리소스 ID 연결.
 */
@Service
@RequiredArgsConstructor
public class IdempotencyKeyService {

    private final IdempotencyKeyRepository idempotencyKeyRepository;

    /**
     * 멱등 해소: 기존 resourceId 반환 또는 {@code createNew}로 신규 생성 후 기록.
     *
     * @param recover  멱등 행에 resourceId가 비어 있을 때, 이미 생성된 리소스가 있는지 조회
     * @param createNew 새 리소스 생성 후 ID 문자열 반환
     */
    @Transactional
    public String resolve(
            String merchantId,
            String idempotencyKey,
            String requestHash,
            Supplier<Optional<String>> recover,
            Supplier<String> createNew
    ) {
        ensureRow(merchantId, idempotencyKey, requestHash);
        IdempotencyKey row = idempotencyKeyRepository
                .findByMerchantIdAndIdempotencyKeyForUpdate(merchantId, idempotencyKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL));
        if (!row.getRequestHash().equals(requestHash)) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_CONFLICT, idempotencyKey);
        }
        if (row.getResourceId() != null) {
            return row.getResourceId();
        }
        Optional<String> recovered = recover.get();
        if (recovered.isPresent()) {
            String id = recovered.get();
            row.assignResource(id);
            return id;
        }
        String id = createNew.get();
        row.assignResource(id);
        return id;
    }

    private void ensureRow(String merchantId, String idempotencyKey, String requestHash) {
        try {
            idempotencyKeyRepository.save(IdempotencyKey.createPending(merchantId, idempotencyKey, requestHash));
            idempotencyKeyRepository.flush();
        } catch (DataIntegrityViolationException ignored) {
        }
    }
}
