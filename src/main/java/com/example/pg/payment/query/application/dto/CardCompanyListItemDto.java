package com.example.pg.payment.query.application.dto;

import com.example.pg.payment.domain.aggregate.CardCompany;

public record CardCompanyListItemDto(
        String code,
        String name
) {
    public static CardCompanyListItemDto from(CardCompany entity) {
        return new CardCompanyListItemDto(
                entity.getCode(),
                entity.getName()
        );
    }
}
