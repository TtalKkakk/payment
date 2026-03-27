package com.example.pg.payment.domain.converter;

import com.example.pg.payment.domain.vo.PaymentCallbackUrl;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PaymentCallbackUrlConverter implements AttributeConverter<PaymentCallbackUrl, String> {
    @Override
    public String convertToDatabaseColumn(PaymentCallbackUrl attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public PaymentCallbackUrl convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        return new PaymentCallbackUrl(dbData);
    }
}
