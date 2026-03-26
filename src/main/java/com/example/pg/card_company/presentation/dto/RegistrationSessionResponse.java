package com.example.pg.card_company.presentation.dto;

public record RegistrationSessionResponse(
        String token,
        String registrationUrl
) {
}
