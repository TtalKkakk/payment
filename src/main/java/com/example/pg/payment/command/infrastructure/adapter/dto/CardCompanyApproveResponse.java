package com.example.pg.payment.command.infrastructure.adapter.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 카드사 결제 승인 응답 (200 OK, body의 success·resultCode로 성공/실패 구분).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CardCompanyApproveResponse(
        boolean success,
        String paymentId,
        String approvalNumber,
        String transactionId,
        String approvedAt,
        String resultCode,
        String message
) {}
