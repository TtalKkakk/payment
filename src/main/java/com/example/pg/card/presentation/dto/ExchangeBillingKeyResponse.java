package com.example.pg.card.presentation.dto;

public record ExchangeBillingKeyResponse(
        String billingKey,
        String ownerId,
        String maskedNumber
) {}
