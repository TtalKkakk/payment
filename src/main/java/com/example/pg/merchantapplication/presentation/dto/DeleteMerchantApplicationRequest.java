package com.example.pg.merchantapplication.presentation.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 신청 삭제 시 사업자번호 + 비밀번호 요청 본문.
 */
public record DeleteMerchantApplicationRequest(
        @NotBlank(message = "사업자번호는 필수입니다.")
        String businessNumber,
        @NotBlank(message = "비밀번호는 필수입니다.")
        String password
) {
}
