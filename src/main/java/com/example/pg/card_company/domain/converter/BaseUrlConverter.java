package com.example.pg.card_company.domain.converter;

import com.example.pg.card_company.domain.vo.BaseUrl;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class BaseUrlConverter implements AttributeConverter<BaseUrl, String> {

    @Override
    public String convertToDatabaseColumn(BaseUrl attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.normalize();
    }

    @Override
    public BaseUrl convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        return new BaseUrl(dbData);
    }
}

