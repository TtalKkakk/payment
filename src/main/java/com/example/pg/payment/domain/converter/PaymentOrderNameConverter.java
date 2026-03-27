package com.example.pg.payment.domain.converter;

import com.example.pg.payment.domain.vo.PaymentOrderName;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PaymentOrderNameConverter implements AttributeConverter<PaymentOrderName, String> {
    @Override
    public String convertToDatabaseColumn(PaymentOrderName attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public PaymentOrderName convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        return new PaymentOrderName(dbData);
    }
}
