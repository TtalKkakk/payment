package com.example.pg.config;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * API 전역 예외 처리 시 공통 응답 body
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String code,
        String message,
        List<FieldErrorDetail> errors
) {
    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, null);
    }

    public static ErrorResponse of(String code, String message, List<FieldErrorDetail> errors) {
        return new ErrorResponse(code, message, errors);
    }

    public record FieldErrorDetail(String field, String reason) {}
}
