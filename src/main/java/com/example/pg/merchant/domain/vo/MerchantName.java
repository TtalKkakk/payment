package com.example.pg.merchant.domain.vo;

import com.example.pg.common.exception.BusinessException;
import com.example.pg.common.exception.ErrorCode;

/**
 * 가맹점명. Merchant·MerchantApplication 양쪽에서 동일한 타입을 사용하여
 * "Merchant.name = MerchantApplication.name"을 설계 레벨에서 보장한다.
 * 검증: 필수, 100자 이하.
 */
public record MerchantName(String value) {

    private static final int MAX_LENGTH = 100;

    public MerchantName {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.APPLICATION_INVALID_INPUT, "가맹점명은 필수이며 공백만 있을 수 없습니다.");
        }
        if (value.length() > MAX_LENGTH) {
            throw new BusinessException(ErrorCode.APPLICATION_INVALID_INPUT, "가맹점명은 " + MAX_LENGTH + "자 이하여야 합니다.");
        }
    }

    public static MerchantName of(String value) {
        return new MerchantName(value);
    }
}
