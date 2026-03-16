package com.example.pg.payment.presentation.dto;

import java.time.LocalDateTime;

public record PaymentApproveResponse(
        boolean success,
        String paymentId,
        String approvalNumber,
        String transactionId,
        LocalDateTime approvedAt,
        String resultCode,
        String message
) {
    public static PaymentApproveResponse success(String paymentId, String approvalNumber, String transactionId,
                                                 LocalDateTime approvedAt) {
        return new PaymentApproveResponse(true, paymentId, approvalNumber, transactionId, approvedAt, "0000", null);
    }

    public static PaymentApproveResponse failure(String paymentId, String resultCode, String message) {
        return new PaymentApproveResponse(false, paymentId, null, null, null, resultCode, message);
    }
}
