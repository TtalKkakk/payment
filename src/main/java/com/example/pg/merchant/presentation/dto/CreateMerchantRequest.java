package com.example.pg.merchant.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateMerchantRequest(
        @NotBlank(message = "가맹점명은 필수입니다")
        String name
) {
}
