package com.example.pg.merchant.presentation.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * API 키 조회 요청. 사업자번호·비밀번호로 신원 확인 후 apiKey·apiSecret 반환 시 사용.
 */
public record CredentialsRequest(
        @NotBlank(message = "사업자번호는 필수입니다.")
        String businessNumber,
        @NotBlank(message = "비밀번호는 필수입니다.")
        String password
) {
}
