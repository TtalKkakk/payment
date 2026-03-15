package com.example.pg.merchant.command.application.dto;

public record CreateMerchantResultDto(
        String apiKey,
        String apiSecret,
        String name
) {
}
