package com.example.pg.payment.domain.converter;

import com.example.pg.payment.domain.vo.PaymentCustomerName;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PaymentCustomerNameConverter implements AttributeConverter<PaymentCustomerName, String> {
    @Override
    public String convertToDatabaseColumn(PaymentCustomerName attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public PaymentCustomerName convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        return new PaymentCustomerName(dbData);
    }
}
