package com.example.pg.card_company.domain.vo;

public record DisplayOrder(int value) {
    public DisplayOrder {
        if (value < 0) {
            throw new IllegalArgumentException("displayOrder must be >= 0");
        }
    }
}

