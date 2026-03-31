package com.example.pg.common.webhook.retry;

import com.example.pg.common.exception.NonRetryableJobException;
import com.example.pg.common.retry.domain.aggregate.RetryJob;
import com.example.pg.common.retry.handler.RetryJobHandler;
import com.example.pg.common.util.HttpOutbound;
import com.example.pg.common.webhook.retry.payload.WebhookDeliveryRetryPayload;
import com.example.pg.common.webhook.retry.payload.WebhookRetryJobTypes;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookDeliveryRetryJobHandler implements RetryJobHandler {

    private final HttpOutbound httpOutbound;
    private final ObjectMapper objectMapper;

    @Override
    public String jobType() {
        return WebhookRetryJobTypes.WEBHOOK_DELIVERY;
    }

    @Override
    public void handle(RetryJob job) throws Exception {
        WebhookDeliveryRetryPayload payload;
        try {
            payload = objectMapper.readValue(job.getPayloadJson(), WebhookDeliveryRetryPayload.class);
        } catch (Exception e) {
            throw new NonRetryableJobException("Invalid payload_json for webhook retry. jobId=" + job.getId(), e);
        }

        if (payload.url() == null || payload.url().isBlank()) {
            throw new NonRetryableJobException("url is required. jobId=" + job.getId());
        }
        if (payload.bodyJson() == null) {
            throw new NonRetryableJobException("bodyJson is required. jobId=" + job.getId());
        }

        try {
            httpOutbound.post(payload.url(), payload.bodyJson(), payload.extraHeaders());
        } catch (HttpClientErrorException e) {
            // 4xx는 재시도해도 의미 없는 경우가 대부분(가맹점 엔드포인트/인증 문제 등)이라 DEAD로 격리
            throw new NonRetryableJobException("Webhook delivery got 4xx. status=" + e.getStatusCode(), e);
        }

        log.info("[WebhookRetry] delivered url={}", payload.url());
    }
}

