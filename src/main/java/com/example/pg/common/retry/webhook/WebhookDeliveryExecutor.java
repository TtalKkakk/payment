package com.example.pg.common.retry.webhook;

import com.example.pg.common.exception.WebhookDeliveryExhaustedException;
import com.example.pg.common.util.HttpOutbound;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.Map;

/**
 * 가맹점 웹훅 HTTP POST 공통 전송 + 재시도.
 * <p>
 * {@link com.example.pg.common.presentation.FranchiseConnect} 구현체에서만 호출되도록 두면
 * 결제·환불·빌링키 등 모든 웹훅이 동일 정책(5xx·타임아웃·연결 실패 시 백오프 재시도)을 공유한다.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookDeliveryExecutor {

    private final HttpOutbound httpOutbound;

    @Retryable(
            retryFor = { HttpServerErrorException.class, ResourceAccessException.class },
            maxAttemptsExpression = "${app.webhook.retry.max-attempts:4}",
            backoff = @Backoff(
                    delayExpression = "${app.webhook.retry.initial-interval-ms:1000}",
                    multiplierExpression = "${app.webhook.retry.multiplier:2}",
                    maxDelayExpression = "${app.webhook.retry.max-delay-ms:30000}"
            )
    )
    public void deliverPost(String url, String bodyJson, Map<String, String> extraHeaders) {
        httpOutbound.post(url, bodyJson, extraHeaders);
    }

    @Recover
    public void recoverAfterRetries(Exception ex, String url, String bodyJson, Map<String, String> extraHeaders) {
        log.error("[Webhook] delivery exhausted retries url={}", url, ex);
        throw new WebhookDeliveryExhaustedException("Webhook delivery failed after retries: " + url, ex);
    }
}
