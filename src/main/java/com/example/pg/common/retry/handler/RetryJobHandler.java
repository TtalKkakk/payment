package com.example.pg.common.retry.handler;

import com.example.pg.common.retry.domain.aggregate.RetryJob;

/**
 * RetryJob 실행 확장 포인트.
 * jobType별로 핸들러를 추가하면 워커가 자동으로 라우팅한다.
 */
public interface RetryJobHandler {
    String jobType();
    void handle(RetryJob job) throws Exception;
}

