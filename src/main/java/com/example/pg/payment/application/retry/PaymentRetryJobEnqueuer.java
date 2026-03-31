package com.example.pg.payment.application.retry;

import com.example.pg.common.retry.job.RetryJobService;
import com.example.pg.payment.application.retry.payload.PaymentCompensateCreationFailurePayload;
import com.example.pg.payment.application.retry.payload.PaymentRetryJobTypes;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentRetryJobEnqueuer {

    private final RetryJobService retryJobService;
    private final ObjectMapper objectMapper;

    public void enqueueCompensateCreationFailure(String paymentId) {
        PaymentCompensateCreationFailurePayload payload = new PaymentCompensateCreationFailurePayload(paymentId);
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            // payload 직렬화 실패는 재시도 자체를 못 걸기 때문에 운영 이슈로 올려야 함
            log.error("[PaymentRetry] payload serialize failed paymentId={}", paymentId, e);
            return;
        }

        retryJobService.upsertPending(
                PaymentRetryJobTypes.PAYMENT_COMPENSATE_CREATION_FAILURE,
                paymentId,
                payloadJson,
                30,
                Duration.ofMinutes(5),
                Duration.ofDays(7)
        );
    }
}

