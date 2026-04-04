package com.example.pg.idempotency.util;

import com.example.pg.common.exception.BusinessException;
import com.example.pg.common.exception.ErrorCode;

public class IdempotencyKeyUtil {
    public static String normalizeIdempotencyKey(String raw) {
        String trimmed = raw.trim();
        if (trimmed.isEmpty() || trimmed.length() > 128) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_INVALID);
        }
        return trimmed;
    }
}
