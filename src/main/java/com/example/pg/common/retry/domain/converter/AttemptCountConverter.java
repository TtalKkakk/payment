package com.example.pg.common.retry.domain.converter;

import com.example.pg.common.retry.domain.vo.AttemptCount;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class AttemptCountConverter implements AttributeConverter<AttemptCount, Integer> {
    @Override
    public Integer convertToDatabaseColumn(AttemptCount attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public AttemptCount convertToEntityAttribute(Integer dbData) {
        return dbData == null ? null : AttemptCount.of(dbData);
    }
}

