package com.example.pg.merchant.domain.converter;

import com.example.pg.merchant.domain.vo.ApiKey;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ApiKeyConverter implements AttributeConverter<ApiKey, String> {
    @Override
    public String convertToDatabaseColumn(ApiKey attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public ApiKey convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        return new ApiKey(dbData);
    }
}
