package com.example.pg.common.retry.webhook.retry.payload;

import java.util.Map;

public record WebhookDeliveryRetryPayload(
        String url,
        String bodyJson,
        Map<String, String> extraHeaders
) {
}

