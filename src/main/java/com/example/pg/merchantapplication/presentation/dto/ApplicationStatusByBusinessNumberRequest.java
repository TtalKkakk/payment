package com.example.pg.merchantapplication.presentation.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 사업자번호로 신청 승인 여부 조회 시 요청 본문.
 * GET 쿼리 파라미터 대신 POST Body로 전달해 로그·URL 노출을 줄인다.
 */
public record ApplicationStatusByBusinessNumberRequest(
        @NotBlank(message = "사업자번호는 필수입니다.")
        String businessNumber
) {
}
