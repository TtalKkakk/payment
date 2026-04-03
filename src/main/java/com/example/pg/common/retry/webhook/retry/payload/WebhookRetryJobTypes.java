package com.example.pg.common.retry.webhook.retry.payload;

import com.example.pg.common.retry.domain.enumerate.RetryJobType;

public final class WebhookRetryJobTypes {
    private WebhookRetryJobTypes() {
    }

    public static final String WEBHOOK_DELIVERY = RetryJobType.WEBHOOK_DELIVERY.name();
}

