package com.example.pg.merchantapplication.domain.vo;

import com.example.pg.common.exception.BusinessException;
import com.example.pg.common.exception.ErrorCode;

/**
 * 비밀번호 해시(BCrypt 등). 검증을 VO가 책임진다.
 */
public record PasswordHash(String value) {

    public PasswordHash {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.APPLICATION_INVALID_INPUT, "비밀번호 해시는 필수입니다.");
        }
    }

    public static PasswordHash of(String value) {
        return new PasswordHash(value);
    }
}
