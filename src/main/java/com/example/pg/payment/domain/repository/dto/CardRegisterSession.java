package com.example.pg.payment.domain.repository.dto;

public record CardRegisterSession(
        String cardCompanyCode,
        String returnUrl,
        String merchantId
) {
}
