package com.example.pg.merchant.domain.converter;

import com.example.pg.merchant.domain.vo.MerchantName;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class MerchantNameConverter implements AttributeConverter<MerchantName, String> {
    @Override
    public String convertToDatabaseColumn(MerchantName attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public MerchantName convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        return MerchantName.of(dbData);
    }
}
