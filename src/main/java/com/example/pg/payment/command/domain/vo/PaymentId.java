package com.example.pg.payment.command.domain.vo;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PaymentId {
    private final String value;

    public static PaymentId generate() {
        return new PaymentId(UUID.randomUUID().toString());
    }

    public static PaymentId from(String value) {
        return new PaymentId(value);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PaymentId)) return false;
        return value.equals(((PaymentId) o).value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
