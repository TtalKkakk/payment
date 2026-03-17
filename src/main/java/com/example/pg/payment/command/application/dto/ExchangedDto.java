package com.example.pg.payment.command.application.dto;

public record ExchangedDto(
        String billingKeyToken,
        String cardCompanyCode
) {
}
