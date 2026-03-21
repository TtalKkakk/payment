package com.example.pg.payment.presentation.dto;

/**
 * 결제 생성 요청 DTO (카드 결제 전용)
 * 영수증 발급, 거래 추적을 위해 가맹점 주문정보·고객정보를 함께 전달한다.
 * callbackUrl: 결제 결과(승인/실패) 수신 웹훅 URL
 */
public record CreatePaymentRequest(
        long amount,
        String merchantOrderId,
        String orderName,
        String customerEmail,
        String customerName,
        String callbackUrl
) {
}

