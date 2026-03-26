package com.example.pg.card_company.application.dto;

public record CardRegisterSession(
        String cardCompanyCode,
        String returnUrl,
        String merchantId
) {
    public CardRegisterSession {
        if (cardCompanyCode == null || cardCompanyCode.isBlank()) {
            throw new IllegalArgumentException("cardCompanyCode must not be null or blank");
        }
        if (merchantId == null || merchantId.isBlank()) {
            throw new IllegalArgumentException("merchantId must not be null or blank");
        }
        if (returnUrl == null || returnUrl.isBlank()) {
            returnUrl = "/";
        }
        if (!returnUrl.contains("?") && returnUrl.endsWith("/")) {
            returnUrl = returnUrl.substring(0, returnUrl.length() - 1);
        }
    }
}