package com.example.pg.common.exception;

/**
 * 카드사 호출의 일시적 실패(5xx, 타임아웃·연결 실패 등).
 * {@link org.springframework.retry.annotation.Retryable} 대상으로만 사용한다.
 */
public class CardCompanyTransientException extends RuntimeException {

    public CardCompanyTransientException(String message, Throwable cause) {
        super(message, cause);
    }
}
