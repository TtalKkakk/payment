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
    public static CardCompanyPort.ApproveResult success(String paymentId, String approvalNumber, String transactionId,
                                                        LocalDateTime approvedAt) {
        return new CardCompanyPort.ApproveResult(true, paymentId, approvalNumber, transactionId, approvedAt, "0000", null);
    }

    public static CardCompanyPort.ApproveResult failure(String paymentId, String resultCode, String message) {
        return new CardCompanyPort.ApproveResult(false, paymentId, null, null, null, resultCode, message);
    }
}
