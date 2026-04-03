package com.example.pg.payment.application.retry;

import com.example.pg.payment.application.retry.payload.PaymentCompensateCreationFailurePayload;
import com.example.pg.payment.application.retry.payload.PaymentRetryJobTypes;
import com.example.pg.payment.application.PaymentService;
import com.example.pg.common.exception.NonRetryableJobException;
import com.example.pg.common.retry.handler.RetryJobHandler;
import com.example.pg.common.retry.domain.aggregate.RetryJob;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCompensateCreationFailureRetryJobHandler implements RetryJobHandler {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    @Override
    public String jobType() {
        return PaymentRetryJobTypes.PAYMENT_COMPENSATE_CREATION_FAILURE;
    }

    @Override
    public void handle(RetryJob job) throws Exception {
        PaymentCompensateCreationFailurePayload payload;
        try {
            payload = objectMapper.readValue(job.getPayloadJson(), PaymentCompensateCreationFailurePayload.class);
        } catch (Exception e) {
            throw new NonRetryableJobException("Invalid payload_json for jobId=" + job.getId(), e);
        }

        if (payload.paymentId() == null || payload.paymentId().isBlank()) {
            throw new NonRetryableJobException("paymentId is required. jobId=" + job.getId());
        }

        paymentService.compensateCreationFailure(payload.paymentId());
        log.info("[PaymentRetry] compensated creation failure paymentId={}", payload.paymentId());
    }
}

