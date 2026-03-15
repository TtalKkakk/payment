package com.example.pg.payment.presentation.port.dto;

public record RegistrationSessionDto(
        String token,
        String registrationUrl
) {
}
