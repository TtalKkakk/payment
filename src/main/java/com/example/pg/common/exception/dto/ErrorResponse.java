package com.example.pg.common.exception.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * API 전역 예외 처리 시 공통 응답 body
 * 결제 실패 시 가맹점 프론트에서 "다시 결제" / "승인만 재시도" 페이지 구분을 위해 retryable, action, paymentId 사용.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String code,
        String message,
        List<FieldErrorDetail> errors,
        Boolean retryable,
        String action,
        String paymentId
) {
    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, null, null, null, null);
    }

    public static ErrorResponse of(String code, String message, List<FieldErrorDetail> errors) {
        return new ErrorResponse(code, message, errors, null, null, null);
    }

    /** 결제 관련 실패: 가맹점 프론트에서 안내 문구·버튼 분기용 */
    public static ErrorResponse ofPaymentFailure(String code, String message, boolean retryable, String action, String paymentId) {
        return new ErrorResponse(code, message, null, retryable, action, paymentId);
    }
}
