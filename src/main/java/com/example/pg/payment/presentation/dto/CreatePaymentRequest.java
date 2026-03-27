package com.example.pg.payment.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 결제 생성 요청 DTO (카드 결제 전용)
 * 영수증 발급, 거래 추적을 위해 가맹점 주문정보·고객정보를 함께 전달한다.
 * callbackUrl: 결제 결과(승인/실패) 수신 웹훅 URL
 * billingKey: 결제 수단이 결정된 빌링키 (결제하기 시점에 전달)
 */
public record CreatePaymentRequest(
        @Positive(message = "amount는 0보다 커야 합니다.")
        long amount,
        @NotBlank(message = "merchantOrderId는 필수입니다.")
        @Size(max = 100, message = "merchantOrderId는 100자 이하여야 합니다.")
        String merchantOrderId,
        @NotBlank(message = "orderName은 필수입니다.")
        @Size(max = 200, message = "orderName은 200자 이하여야 합니다.")
        String orderName,
        @Email(message = "customerEmail 형식이 올바르지 않습니다.")
        @Size(max = 200, message = "customerEmail은 200자 이하여야 합니다.")
        String customerEmail,
        @NotBlank(message = "customerName은 필수입니다.")
        @Size(max = 100, message = "customerName은 100자 이하여야 합니다.")
        String customerName,
        @NotBlank(message = "callbackUrl은 필수입니다.")
        @Size(max = 500, message = "callbackUrl은 500자 이하여야 합니다.")
        String callbackUrl,
        @NotBlank(message = "billingKey는 필수입니다.")
        String billingKey,
        @NotBlank(message = "cardCompanyCode는 필수입니다.")
        @Size(max = 50, message = "cardCompanyCode는 50자 이하여야 합니다.")
        String cardCompanyCode
) {
}

