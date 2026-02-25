package com.example.pg.card.command.application.result;

public record CardRegistrationResult(
    String authCode,
    String returnUrl
) {}