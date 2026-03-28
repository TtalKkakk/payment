package com.example.pg.merchantapplication.domain.converter;

import com.example.pg.merchantapplication.domain.vo.BusinessNumber;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class BusinessNumberConverter implements AttributeConverter<BusinessNumber, String> {

    @Override
    public String convertToDatabaseColumn(BusinessNumber attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public BusinessNumber convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        return BusinessNumber.of(dbData);
    }
}
