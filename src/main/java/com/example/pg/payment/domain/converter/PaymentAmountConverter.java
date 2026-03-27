package com.example.pg.payment.domain.converter;

import com.example.pg.payment.domain.vo.PaymentAmount;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PaymentAmountConverter implements AttributeConverter<PaymentAmount, Long> {
    @Override
    public Long convertToDatabaseColumn(PaymentAmount attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public PaymentAmount convertToEntityAttribute(Long dbData) {
        if (dbData == null) {
            return null;
        }
        return new PaymentAmount(dbData);
    }
}
