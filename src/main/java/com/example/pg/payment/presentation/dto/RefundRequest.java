package com.example.pg.payment.presentation.dto;

/**
 * 카드사 결제 환불(취소) 요청.
 * POST /api/pg/payments/refund Body.
 */
public record RefundRequest(String paymentId) {}
