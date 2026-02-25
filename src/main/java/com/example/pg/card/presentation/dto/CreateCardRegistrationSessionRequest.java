package com.example.pg.card.presentation.dto;

public record CreateCardRegistrationSessionRequest(
        String ownerId,
        String returnUrl
) {
}