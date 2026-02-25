package com.example.pg.merchant.command.application.dto;

public record RegenerateSecretResult(
        String apiKey,
        String apiSecret
) {
}
