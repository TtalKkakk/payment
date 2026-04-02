package com.example.pg.payment.presentation.dto;

import com.example.pg.payment.domain.aggregate.Payment;

import java.time.LocalDateTime;

/**
 * 결제 단건 조회 응답.
 * 폴링·상태 확인·운영 조회 등에 사용한다.
 */
public record PaymentDetailResponse(
        String paymentId,
        String status,
        String merchantId,
        String merchantOrderId,
        long amount,
        String orderName,
        String customerEmail,
        String customerName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String lastFailureCategory,
        String lastFailureCode,
        String lastFailureMessage,
        LocalDateTime lastFailureAt
) {
    public static PaymentDetailResponse from(Payment payment) {
        return new PaymentDetailResponse(
                payment.getId(),
                payment.getStatus().name(),
                payment.getMerchantId(),
                payment.getMerchantOrderId(),
                payment.getAmount(),
                payment.getOrderName(),
                payment.getCustomerEmail(),
                payment.getCustomerName(),
                payment.getCreatedAt(),
                payment.getUpdatedAt(),
                payment.getLastFailureCategory() == null ? null : payment.getLastFailureCategory().name(),
                payment.getLastFailureCode(),
                payment.getLastFailureMessage(),
                payment.getLastFailureAt()
        );
    }
}
