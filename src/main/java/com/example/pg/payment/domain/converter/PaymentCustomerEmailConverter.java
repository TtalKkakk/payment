package com.example.pg.payment.domain.converter;

import com.example.pg.payment.domain.vo.PaymentCustomerEmail;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PaymentCustomerEmailConverter implements AttributeConverter<PaymentCustomerEmail, String> {
    @Override
    public String convertToDatabaseColumn(PaymentCustomerEmail attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public PaymentCustomerEmail convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        return new PaymentCustomerEmail(dbData);
    }
}
