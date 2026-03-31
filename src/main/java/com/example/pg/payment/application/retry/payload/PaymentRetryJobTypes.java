package com.example.pg.payment.application.retry.payload;

import com.example.pg.common.retry.domain.enumerate.RetryJobType;

public final class PaymentRetryJobTypes {
    private PaymentRetryJobTypes() {
    }

    public static final String PAYMENT_COMPENSATE_CREATION_FAILURE = RetryJobType.PAYMENT_COMPENSATE_CREATION_FAILURE.name();
}

