package com.example.pg.payment.command.application.port.dto;

import com.example.pg.payment.command.application.port.CardCompanyPort;

import java.time.LocalDateTime;

public record PaymentApproveDto(
        boolean success,
        String paymentId,
        String approvalNumber,
        String transactionId,
        LocalDateTime approvedAt,
        String resultCode,
        String message
) {
    public static PaymentApproveDto success(String paymentId, String approvalNumber, String transactionId,
                                                        LocalDateTime approvedAt) {
        return new PaymentApproveDto(true, paymentId, approvalNumber, transactionId, approvedAt, "0000", null);
    }

    public static PaymentApproveDto failure(String paymentId, String resultCode, String message) {
        return new PaymentApproveDto(false, paymentId, null, null, null, resultCode, message);
    }
}
