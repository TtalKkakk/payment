package com.example.pg.merchantapplication.domain.vo;

import com.example.pg.common.exception.BusinessException;
import com.example.pg.common.exception.ErrorCode;

/**
 * 사업자번호. 형식 XXX-XX-XXXXX. 검증을 VO가 책임진다.
 */
public record BusinessNumber(String value) {

    private static final String PATTERN = "^[0-9]{3}-[0-9]{2}-[0-9]{5}$";
    private static final int MAX_LENGTH = 20;

    public BusinessNumber {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.APPLICATION_INVALID_INPUT, "사업자번호는 필수입니다.");
        }
        if (!value.matches(PATTERN)) {
            throw new BusinessException(ErrorCode.APPLICATION_INVALID_INPUT, "사업자번호 형식이 올바르지 않습니다. (XXX-XX-XXXXX)");
        }
        if (value.length() > MAX_LENGTH) {
            throw new BusinessException(ErrorCode.APPLICATION_INVALID_INPUT, "사업자번호는 " + MAX_LENGTH + "자 이하여야 합니다.");
        }
    }

    public static BusinessNumber of(String value) {
        return new BusinessNumber(value);
    }
}
