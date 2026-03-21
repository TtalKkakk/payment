package com.example.pg.card.presentation.dto;

public record CreateCardRegistrationRequest(
        String token,
        String cardNumber,
        String expiry,
        String cvc
) {
}
