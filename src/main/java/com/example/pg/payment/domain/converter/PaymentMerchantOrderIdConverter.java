package com.example.pg.payment.domain.converter;

import com.example.pg.payment.domain.vo.PaymentMerchantOrderId;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PaymentMerchantOrderIdConverter implements AttributeConverter<PaymentMerchantOrderId, String> {
    @Override
    public String convertToDatabaseColumn(PaymentMerchantOrderId attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public PaymentMerchantOrderId convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        return new PaymentMerchantOrderId(dbData);
    }
}
