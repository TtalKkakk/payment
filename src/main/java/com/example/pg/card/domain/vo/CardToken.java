package com.example.pg.card.domain.vo;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

/**
 * card token 값을 관리하는 객체 -> 추후에 bank로 옮기고 해당 토큰은 결제 메타 데이터 uuid로 대체
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CardToken {

    private final String value;

    public static CardToken generate() {
        return new CardToken(UUID.randomUUID().toString());
    }

    public static CardToken from(String value) {
        return new CardToken(value);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CardToken)) return false;
        return value.equals(((CardToken) o).value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}

