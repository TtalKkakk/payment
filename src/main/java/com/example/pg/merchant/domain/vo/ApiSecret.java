package com.example.pg.merchant.domain.vo;

import java.util.Objects;
import java.util.UUID;

/**
 * 가맹점 API 시크릿. 형식 sk_merchant_ + 24자.
 * 생성·비교 로직을 VO에 두어 일원화한다.
 */
public record ApiSecret(String value) {

    private static final String PREFIX = "sk_merchant_";
    private static final int RANDOM_LENGTH = 24;

    public ApiSecret {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ApiSecret value must not be blank");
        }
        if (!value.startsWith(PREFIX) || value.length() != PREFIX.length() + RANDOM_LENGTH) {
            throw new IllegalArgumentException("ApiSecret must be " + PREFIX + "<24 chars>");
        }
    }

    public static ApiSecret ofRandom() {
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, RANDOM_LENGTH);
        return new ApiSecret(PREFIX + random);
    }

    public boolean matches(String plain) {
        return Objects.equals(this.value, plain);
    }
}
