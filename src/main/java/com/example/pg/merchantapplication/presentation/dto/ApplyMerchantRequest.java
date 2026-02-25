package com.example.pg.merchantapplication.presentation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ApplyMerchantRequest(
        @NotBlank(message = "가맹점명은 필수입니다")
        String name,
        @NotBlank(message = "사업자번호는 필수입니다")
        @Pattern(regexp = "^[0-9]{3}-[0-9]{2}-[0-9]{5}$", message = "사업자번호 형식이 올바르지 않습니다 (XXX-XX-XXXXX)")
        String businessNumber,
        @NotBlank(message = "연락처는 필수입니다")
        String phone,
        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        String email,
        @NotBlank(message = "비밀번호는 필수입니다")
        @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다")
        String password
) {
}
