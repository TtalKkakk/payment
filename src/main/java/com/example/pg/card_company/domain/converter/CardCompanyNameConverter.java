package com.example.pg.card_company.domain.converter;

import com.example.pg.card_company.domain.vo.CardCompanyName;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CardCompanyNameConverter implements AttributeConverter<CardCompanyName, String> {

    @Override
    public String convertToDatabaseColumn(CardCompanyName attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public CardCompanyName convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        return new CardCompanyName(dbData);
    }
}

