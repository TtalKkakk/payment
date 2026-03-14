package com.example.pg.receipt.domain.vo;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ReceiptId {
    private final String value;

    public static ReceiptId generate() {
        return new ReceiptId(UUID.randomUUID().toString());
    }

    public static ReceiptId from(String value) {
        return new ReceiptId(value);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ReceiptId)) return false;
        return value.equals(((ReceiptId) o).value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
