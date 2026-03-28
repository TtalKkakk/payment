package com.example.pg.merchantapplication.domain.converter;

import com.example.pg.merchantapplication.domain.vo.ContactEmail;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ContactEmailConverter implements AttributeConverter<ContactEmail, String> {

    @Override
    public String convertToDatabaseColumn(ContactEmail attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public ContactEmail convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        return ContactEmail.of(dbData);
    }
}
