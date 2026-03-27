package com.example.pg.merchant.domain.vo;

import java.util.UUID;
import java.util.Optional;

/**
 * 가맹점 API 키. 형식 pk_merchant_ + 24자.
 * 생성 로직을 VO에 두어 일원화한다.
 */
public record ApiKey(String value) {

    private static final String PREFIX = "pk_merchant_";
    private static final int RANDOM_LENGTH = 24;

    public ApiKey {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ApiKey value must not be blank");
        }
        if (!value.startsWith(PREFIX) || value.length() != PREFIX.length() + RANDOM_LENGTH) {
            throw new IllegalArgumentException("ApiKey must be " + PREFIX + "<24 chars>");
        }
    }

    public static ApiKey ofRandom() {
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, RANDOM_LENGTH);
        return new ApiKey(PREFIX + random);
    }

    /**
     * 외부 입력 문자열을 ApiKey로 안전하게 파싱한다.
     * 형식이 맞지 않으면 예외 대신 Optional.empty()를 반환한다.
     */
    public static Optional<ApiKey> tryParse(String value) {
        try {
            return Optional.of(new ApiKey(value));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
