package com.example.pg.merchant.application.dto;

public record CreateMerchantResultDto(
        String apiKey,
        String apiSecret,
        String name
) {
}
