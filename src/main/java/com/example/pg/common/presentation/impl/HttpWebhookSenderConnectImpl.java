package com.example.pg.common.presentation.impl;

import com.example.pg.common.exception.WebhookDeliveryExhaustedException;
import com.example.pg.common.webhook.WebhookDeliveryExecutor;
import com.example.pg.common.presentation.FranchiseConnect;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 웹훅 발송 HTTP 어댑터.
 * {@link WebhookDeliveryExecutor}로 POST하며, 재시도 정책은 결제·빌링키 등 모든 {@link FranchiseConnect} 호출에 공통 적용된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HttpWebhookSenderConnectImpl implements FranchiseConnect {

    private static final String SIGNATURE_HEADER = "X-PG-Signature";

    private final WebhookDeliveryExecutor webhookDeliveryExecutor;

    @Override
    public void send(String url, String bodyJson, String signatureValue) {
        Map<String, String> extraHeaders = null;
        if (signatureValue != null && !signatureValue.isBlank()) {
            extraHeaders = Map.of(SIGNATURE_HEADER, signatureValue);
        }
        try {
            webhookDeliveryExecutor.deliverPost(url, bodyJson, extraHeaders);
            log.info("[Webhook] delivery done url={}", url);
        } catch (WebhookDeliveryExhaustedException e) {
            log.error("[Webhook] delivery failed after retries url={}", url, e);
        } catch (Exception e) {
            log.error("[Webhook] delivery failed (no retry or non-retryable) url={}", url, e);
        }
    }
}
