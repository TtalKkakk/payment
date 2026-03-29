package com.example.pg.payment.presentation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 카드사 → PG 결제 환불(취소) 응답.
 * 성공 시 환불 금액·원거래 정보, 실패 시 결과 코드·메시지. {@link PaymentApproveResponse}와 동일한 패턴.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaymentRefundResponse(
        boolean success,
        String paymentId,
        String approvalNumber,
        String transactionId,
        LocalDateTime approvedAt,
        BigDecimal amount,
        String resultCode,
        String message
) {
    public static PaymentRefundResponse success(
            String paymentId,
            String approvalNumber,
            String transactionId,
            LocalDateTime approvedAt,
            BigDecimal amount
    ) {
        return new PaymentRefundResponse(
                true,
                paymentId,
                approvalNumber,
                transactionId,
                approvedAt,
                amount,
                "0000",
                null
        );
    }

    public static PaymentRefundResponse failure(String paymentId, String resultCode, String message) {
        return new PaymentRefundResponse(
                false,
                paymentId,
                null,
                null,
                null,
                null,
                resultCode,
                message
        );
    }
}
