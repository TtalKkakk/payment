package com.example.pg.common.retry.domain.converter;

import com.example.pg.common.retry.domain.vo.JobType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class JobTypeConverter implements AttributeConverter<JobType, String> {
    @Override
    public String convertToDatabaseColumn(JobType attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public JobType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : JobType.of(dbData);
    }
}

