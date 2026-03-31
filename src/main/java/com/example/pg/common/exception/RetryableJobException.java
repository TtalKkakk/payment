package com.example.pg.common.exception;

public class RetryableJobException extends RuntimeException {
    public RetryableJobException(String message) {
        super(message);
    }

    public RetryableJobException(String message, Throwable cause) {
        super(message, cause);
    }
}

