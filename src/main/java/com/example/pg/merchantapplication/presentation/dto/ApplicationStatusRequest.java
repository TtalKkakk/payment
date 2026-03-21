package com.example.pg.merchantapplication.presentation.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 사업자번호 + 비밀번호로 심사 상세 조회 시 요청 본문.
 * 사업자번호와 비밀번호가 모두 일치해야 조회 가능.
 */
public record ApplicationStatusRequest(
        @NotBlank(message = "사업자번호는 필수입니다.")
        String businessNumber,
        @NotBlank(message = "비밀번호는 필수입니다.")
        String password
) {
}
