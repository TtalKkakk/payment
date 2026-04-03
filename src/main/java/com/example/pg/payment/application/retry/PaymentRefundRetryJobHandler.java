package com.example.pg.payment.application.retry;

import com.example.pg.common.exception.NonRetryableJobException;
import com.example.pg.common.retry.domain.aggregate.RetryJob;
import com.example.pg.common.retry.handler.RetryJobHandler;
import com.example.pg.payment.application.PaymentRefundProcessor;
import com.example.pg.payment.application.retry.payload.PaymentRefundRetryPayload;
import com.example.pg.payment.application.retry.payload.PaymentRetryJobTypes;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentRefundRetryJobHandler implements RetryJobHandler {

    private final PaymentRefundProcessor paymentRefundProcessor;
    private final ObjectMapper objectMapper;

    @Override
    public String jobType() {
        return PaymentRetryJobTypes.PAYMENT_REFUND_RETRY;
    }

    @Override
    public void handle(RetryJob job) {
        PaymentRefundRetryPayload payload;
        try {
            payload = objectMapper.readValue(job.getPayloadJson(), PaymentRefundRetryPayload.class);
        } catch (Exception e) {
            throw new NonRetryableJobException("Invalid payload_json for jobId=" + job.getId(), e);
        }

        if (payload.paymentId() == null || payload.paymentId().isBlank()) {
            throw new NonRetryableJobException("paymentId is required. jobId=" + job.getId());
        }

        paymentRefundProcessor.processRefundRetryFromJob(payload.paymentId());
        log.info("[PaymentRetry] refund retry job executed paymentId={}", payload.paymentId());
    }
}
