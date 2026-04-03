package com.example.pg.common.retry.card_company;

import com.example.pg.card_company.presentation.CardCompanyConnect;
import com.example.pg.common.exception.CardCompanyTransientException;
import com.example.pg.payment.presentation.dto.PaymentApproveResponse;
import com.example.pg.payment.presentation.dto.PaymentRefundResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * 카드사 승인/환불 호출의 단기 재시도(일시 장애·5xx·연결·타임아웃).
 * 카드사가 HTTP 200으로 내려준 {@code success=false}는 어댑터가 예외로 바꾸지 않으므로 재시도 대상이 아니다.
 */
@Slf4j
@Service
public class CardCompanyPaymentRetryExecutor {

    @Retryable(
            retryFor = CardCompanyTransientException.class,
            maxAttemptsExpression = "${app.card-company.retry.max-attempts:4}",
            backoff = @Backoff(
                    delayExpression = "${app.card-company.retry.initial-interval-ms:500}",
                    multiplierExpression = "${app.card-company.retry.multiplier:2}",
                    maxDelayExpression = "${app.card-company.retry.max-delay-ms:8000}"
            )
    )
    public PaymentApproveResponse approve(CardCompanyConnect port, String paymentId, long amount, String billingKey) {
        return port.approve(paymentId, amount, billingKey);
    }

    @Retryable(
            retryFor = CardCompanyTransientException.class,
            maxAttemptsExpression = "${app.card-company.retry.max-attempts:4}",
            backoff = @Backoff(
                    delayExpression = "${app.card-company.retry.initial-interval-ms:500}",
                    multiplierExpression = "${app.card-company.retry.multiplier:2}",
                    maxDelayExpression = "${app.card-company.retry.max-delay-ms:8000}"
            )
    )
    public PaymentRefundResponse refund(CardCompanyConnect port, String paymentId) {
        return port.requestRefund(paymentId);
    }

    @Recover
    public PaymentApproveResponse recoverApprove(CardCompanyTransientException ex,
                                                 CardCompanyConnect port,
                                                 String paymentId,
                                                 long amount,
                                                 String billingKey) {
        log.warn("[CardCompany] approve retries exhausted paymentId={}", paymentId, ex);
        throw ex;
    }

    @Recover
    public PaymentRefundResponse recoverRefund(CardCompanyTransientException ex,
                                               CardCompanyConnect port,
                                               String paymentId) {
        log.warn("[CardCompany] refund retries exhausted paymentId={}", paymentId, ex);
        throw ex;
    }
}
