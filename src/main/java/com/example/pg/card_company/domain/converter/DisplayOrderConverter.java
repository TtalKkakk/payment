package com.example.pg.card_company.domain.converter;

import com.example.pg.card_company.domain.vo.DisplayOrder;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class DisplayOrderConverter implements AttributeConverter<DisplayOrder, Integer> {

    @Override
    public Integer convertToDatabaseColumn(DisplayOrder attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public DisplayOrder convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return null;
        }
        return new DisplayOrder(dbData);
    }
}

