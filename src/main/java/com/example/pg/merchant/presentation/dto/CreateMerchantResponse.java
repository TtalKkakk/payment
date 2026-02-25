package com.example.pg.merchant.presentation.dto;

public record CreateMerchantResponse(
        String apiKey,
        String apiSecret,
        String name
) {
}
