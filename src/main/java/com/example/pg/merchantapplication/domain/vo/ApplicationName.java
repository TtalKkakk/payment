package com.example.pg.merchantapplication.domain.vo;

import com.example.pg.common.exception.BusinessException;
import com.example.pg.common.exception.ErrorCode;

/**
 * 가맹점 신청 시 가맹점명. 검증을 VO가 책임진다.
 */
public record ApplicationName(String value) {

    private static final int MAX_LENGTH = 100;

    public ApplicationName {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.APPLICATION_INVALID_INPUT, "가맹점명은 필수이며 공백만 있을 수 없습니다.");
        }
        if (value.length() > MAX_LENGTH) {
            throw new BusinessException(ErrorCode.APPLICATION_INVALID_INPUT, "가맹점명은 " + MAX_LENGTH + "자 이하여야 합니다.");
        }
    }

    public static ApplicationName of(String value) {
        return new ApplicationName(value);
    }
}
