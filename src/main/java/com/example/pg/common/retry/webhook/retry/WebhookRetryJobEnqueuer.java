package com.example.pg.common.retry.webhook.retry;

import com.example.pg.common.retry.job.RetryJobService;
import com.example.pg.common.retry.webhook.retry.payload.WebhookDeliveryRetryPayload;
import com.example.pg.common.retry.webhook.retry.payload.WebhookRetryJobTypes;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookRetryJobEnqueuer {

    private final RetryJobService retryJobService;
    private final ObjectMapper objectMapper;

    public void enqueue(String url, String bodyJson, Map<String, String> extraHeaders) {
        Map<String, String> normalizedHeaders = extraHeaders == null ? null : new TreeMap<>(extraHeaders);
        WebhookDeliveryRetryPayload payload = new WebhookDeliveryRetryPayload(url, bodyJson, normalizedHeaders);

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("[WebhookRetry] payload serialize failed url={}", url, e);
            return;
        }

        String idempotencyKey = computeIdempotencyKey(url, bodyJson, normalizedHeaders);

        retryJobService.upsertPending(
                WebhookRetryJobTypes.WEBHOOK_DELIVERY,
                idempotencyKey,
                payloadJson,
                60,
                Duration.ofMinutes(10),
                Duration.ofDays(14)
        );
    }

    private static String computeIdempotencyKey(String url, String bodyJson, Map<String, String> headers) {
        // 동일 이벤트가 중복 enqueue되더라도 1개의 Job으로 수렴시키기 위한 키.
        // (도메인 이벤트 ID가 없는 상태에서의 안전한 기본값)
        StringBuilder sb = new StringBuilder();
        sb.append(url == null ? "" : url).append('\n');
        sb.append(bodyJson == null ? "" : bodyJson).append('\n');
        if (headers != null) {
            headers.forEach((k, v) -> sb.append(k).append('=').append(v).append('\n'));
        }
        return "WEBHOOK:" + sha256Hex(sb.toString());
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            // SHA-256 미지원은 현실적으로 없으므로, 마지막 방어로 fallback
            return Integer.toHexString(value.hashCode());
        }
    }
}

