package com.example.pg.payment.command.infrastructure.adapter.dto;

/**
 * 카드사 결제 승인 요청 (POST /api/pg/payments/approve).
 */
public record CardCompanyApproveRequest(
        String paymentId,
        long amount,
        String billingKeyToken
) {}
