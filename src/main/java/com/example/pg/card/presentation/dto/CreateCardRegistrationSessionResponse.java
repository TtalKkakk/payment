package com.example.pg.card.presentation.dto;

public record CreateCardRegistrationSessionResponse(
        String token,
        String registrationUrl
) {
}

