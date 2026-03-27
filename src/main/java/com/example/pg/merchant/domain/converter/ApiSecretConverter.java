package com.example.pg.merchant.domain.converter;

import com.example.pg.merchant.domain.vo.ApiSecret;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ApiSecretConverter implements AttributeConverter<ApiSecret, String> {
    @Override
    public String convertToDatabaseColumn(ApiSecret attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public ApiSecret convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        return new ApiSecret(dbData);
    }
}
