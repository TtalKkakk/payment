package com.example.pg.card_company.util.dto;

import com.example.pg.card_company.domain.aggergate.CardCompany;

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
