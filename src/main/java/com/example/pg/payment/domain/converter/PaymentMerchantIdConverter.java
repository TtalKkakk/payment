package com.example.pg.payment.domain.converter;

import com.example.pg.payment.domain.vo.PaymentMerchantId;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PaymentMerchantIdConverter implements AttributeConverter<PaymentMerchantId, String> {
    @Override
    public String convertToDatabaseColumn(PaymentMerchantId attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public PaymentMerchantId convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        return new PaymentMerchantId(dbData);
    }
}
