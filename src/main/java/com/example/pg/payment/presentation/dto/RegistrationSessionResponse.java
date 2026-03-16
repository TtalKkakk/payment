package com.example.pg.payment.presentation.dto;

public record RegistrationSessionResponse(
        String token,
        String registrationUrl
) {
}
