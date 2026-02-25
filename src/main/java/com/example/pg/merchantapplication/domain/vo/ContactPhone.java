package com.example.pg.merchantapplication.domain.vo;

import com.example.pg.exception.BusinessException;
import com.example.pg.exception.ErrorCode;

/**
 * 연락처. 검증을 VO가 책임진다.
 */
public record ContactPhone(String value) {

    private static final int MAX_LENGTH = 50;

    public ContactPhone {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.APPLICATION_INVALID_INPUT, "연락처는 필수입니다.");
        }
        if (value.length() > MAX_LENGTH) {
            throw new BusinessException(ErrorCode.APPLICATION_INVALID_INPUT, "연락처는 " + MAX_LENGTH + "자 이하여야 합니다.");
        }
    }

    public static ContactPhone of(String value) {
        return new ContactPhone(value);
    }
}
