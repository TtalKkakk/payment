package com.example.pg.cardcompany.query.application.dto;

import com.example.pg.cardcompany.domain.aggregate.CardCompany;

public record CardCompanyListItem(
        String code,
        String name
) {
    public static CardCompanyListItem from(CardCompany entity) {
        return new CardCompanyListItem(
                entity.getCode(),
                entity.getName()
        );
    }
}
