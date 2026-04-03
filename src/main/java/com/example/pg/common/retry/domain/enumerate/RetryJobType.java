package com.example.pg.common.retry.domain.enumerate;

/**
 * jobType은 외부 시스템/도메인에 독립적인 "워크 아이템 타입" 식별자.
 * 필요 시 enum 대신 문자열 상수로 운용해도 된다.
 */
public enum RetryJobType {
    PAYMENT_COMPENSATE_CREATION_FAILURE,
    PAYMENT_REFUND_RETRY,
    WEBHOOK_DELIVERY
}

