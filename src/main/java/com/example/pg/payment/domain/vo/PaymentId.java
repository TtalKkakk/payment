package com.example.pg.payment.domain.vo;

import com.example.pg.common.exception.BusinessException;
import com.example.pg.common.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PaymentId {
    private final UUID value;

    public static PaymentId generate() {
        return new PaymentId(UUID.randomUUID());
    }

    public static PaymentId from(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.PAYMENT_ID_INVALID);
        }
        try {
            return new PaymentId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.PAYMENT_ID_INVALID);
        }
    }

    public String getValue() {
        return value.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PaymentId)) return false;
        return value.equals(((PaymentId) o).value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
