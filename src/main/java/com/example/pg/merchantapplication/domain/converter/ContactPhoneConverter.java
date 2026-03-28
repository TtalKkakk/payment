package com.example.pg.merchantapplication.domain.converter;

import com.example.pg.merchantapplication.domain.vo.ContactPhone;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ContactPhoneConverter implements AttributeConverter<ContactPhone, String> {

    @Override
    public String convertToDatabaseColumn(ContactPhone attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public ContactPhone convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        return ContactPhone.of(dbData);
    }
}
