package com.example.pg.payment.presentation.dto;

/**
 * 카드사 결제 환불(취소) 응답.
 * 200 OK Body. 실패 시 4xx + ErrorResponse(code, message).
 */
public record RefundResponse(boolean success, String paymentId) {}
