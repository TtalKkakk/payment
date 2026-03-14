package com.example.pg.payment.command.application.port.dto;

import com.example.pg.payment.command.application.port.MerchantPort;

import java.time.LocalDateTime;

/**
 * 영수증 발급에 필요한 결제 스냅샷 (안티커럽션).
 * payment 도메인에 의존하지 않고 영수증 도메인이 사용하는 DTO.
 * 가맹점명은 {@link MerchantPort}로 별도 조회한다.
 */
public record PaymentSnapshotForReceiptDto(
        String paymentId,
        String merchantId,
        long amount,
        String orderName,
        String merchantOrderId,
        String customerName,
        String approvalNumber,
        String transactionId,
        LocalDateTime approvedAt
) {
}
