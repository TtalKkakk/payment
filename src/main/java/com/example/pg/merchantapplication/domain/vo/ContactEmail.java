package com.example.pg.merchantapplication.domain.vo;

import com.example.pg.exception.BusinessException;
import com.example.pg.exception.ErrorCode;

/**
 * 이메일. 검증을 VO가 책임진다.
 */
public record ContactEmail(String value) {

    private static final int MAX_LENGTH = 100;

    public ContactEmail {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.APPLICATION_INVALID_INPUT, "이메일은 필수입니다.");
        }
        if (value.length() > MAX_LENGTH) {
            throw new BusinessException(ErrorCode.APPLICATION_INVALID_INPUT, "이메일은 " + MAX_LENGTH + "자 이하여야 합니다.");
        }
        if (!value.contains("@") || value.indexOf('@') == 0 || !value.contains(".")) {
            throw new BusinessException(ErrorCode.APPLICATION_INVALID_INPUT, "이메일 형식이 올바르지 않습니다.");
        }
    }

    public static ContactEmail of(String value) {
        return new ContactEmail(value);
    }
}
