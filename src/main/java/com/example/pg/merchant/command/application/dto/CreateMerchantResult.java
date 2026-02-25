package com.example.pg.merchant.command.application.dto;

public record CreateMerchantResult(
        String apiKey,
        String apiSecret,
        String name
) {
}
