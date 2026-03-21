package com.example.pg.card.command.application.result;

public record CreateSessionResult(
        String token,
        String registrationUrl
) {}