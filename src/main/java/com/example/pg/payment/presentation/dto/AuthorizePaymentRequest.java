package com.example.pg.payment.presentation.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 결제 승인 요청 DTO (빌링키 방식)
 * 배달앱이 보유한 빌링키를 전달하여 결제를 진행한다.
 */
public record AuthorizePaymentRequest(
        @NotBlank(message = "billingKey는 필수입니다.")
        String billingKey
) {
}
