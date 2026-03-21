package com.example.pg.card.command.application.result;

public record BillingKeyExchangeResult(
        String billingKey,
        String ownerId,
        String maskedNumber
) {}
