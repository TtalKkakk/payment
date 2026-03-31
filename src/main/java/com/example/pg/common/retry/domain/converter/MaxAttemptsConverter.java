package com.example.pg.common.retry.domain.converter;

import com.example.pg.common.retry.domain.vo.MaxAttempts;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class MaxAttemptsConverter implements AttributeConverter<MaxAttempts, Integer> {
    @Override
    public Integer convertToDatabaseColumn(MaxAttempts attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public MaxAttempts convertToEntityAttribute(Integer dbData) {
        return dbData == null ? null : MaxAttempts.of(dbData);
    }
}

