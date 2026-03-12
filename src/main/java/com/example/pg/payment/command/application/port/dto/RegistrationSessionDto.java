package com.example.pg.payment.command.application.port.dto;

public record RegistrationSessionDto(
        String token,
        String registrationUrl
) {
}
