package com.example.pg.common.webhook;

/**
 * 가맹점 웹훅 전송이 설정된 재시도 횟수를 모두 소진한 뒤에도 성공하지 못했을 때 던진다.
 * 이후 DLQ·수동 재처리 등은 이 예외를 구독하거나 호출부에서 처리하도록 확장할 수 있다.
 */
public class WebhookDeliveryExhaustedException extends RuntimeException {

    public WebhookDeliveryExhaustedException(String message, Throwable cause) {
        super(message, cause);
    }
}
