package com.example.pg.cardcompany.query.application.dto;

import com.example.pg.cardcompany.domain.aggregate.CardCompany;

/**
 * 카드사 목록/선택용 조회 결과.
 */
public record CardCompanyListItem(
        String id,
        String code,
        String name,
        String baseUrl
) {
    public static CardCompanyListItem from(CardCompany entity) {
        return new CardCompanyListItem(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getBaseUrl()
        );
    }
}
