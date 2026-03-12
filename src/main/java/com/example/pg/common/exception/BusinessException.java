package com.example.pg.common.exception;

import lombok.Getter;

/**
 * 비즈니스 규칙 위반 시 사용. ErrorCode로 HTTP 상태·에러 코드·메시지를 일괄 관리.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Object[] args;

    public BusinessException(ErrorCode errorCode, Object... args) {
        super(errorCode.formatMessage(args));
        this.errorCode = errorCode;
        this.args = args;
    }
}
