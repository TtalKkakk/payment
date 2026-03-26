package com.example.pg.card_company.domain.converter;

import com.example.pg.card_company.domain.vo.CardCompanyCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CardCompanyCodeConverter implements AttributeConverter<CardCompanyCode, String> {

    @Override
    public String convertToDatabaseColumn(CardCompanyCode attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public CardCompanyCode convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        return new CardCompanyCode(dbData);
    }
}

