package com.example.pg.card.infrastructure.persistence;

import com.example.pg.card.domain.aggregate.AuthCode;
import com.example.pg.card.domain.repository.AuthCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * AuthCode를 Redis에 저장. TTL 5분. 1회 사용 후 used 플래그 갱신.
 */
@Repository
@RequiredArgsConstructor
public class RedisAuthCodeRepository implements AuthCodeRepository {

    private static final String KEY_PREFIX = "auth_code:";
    private static final String MERCHANT_PREFIX = "auth_code:merchant:";
    private static final Duration TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redis;

    @Override
    public void save(AuthCode authCode) {
        String key = KEY_PREFIX + authCode.getCode();
        redis.opsForHash().put(key, "code", authCode.getCode());
        redis.opsForHash().put(key, "cardToken", authCode.getCardToken());
        redis.opsForHash().put(key, "merchantId", authCode.getMerchantId());
        redis.opsForHash().put(key, "ownerId", authCode.getOwnerId());
        redis.opsForHash().put(key, "maskedNumber", authCode.getMaskedNumber());
        redis.opsForHash().put(key, "expiresAt", authCode.getExpiresAt().toString());
        redis.opsForHash().put(key, "used", String.valueOf(authCode.isUsed()));
        redis.opsForHash().put(key, "createdAt", authCode.getCreatedAt().toString());
        redis.expire(key, TTL);

        String merchantKey = MERCHANT_PREFIX + authCode.getMerchantId();
        redis.opsForSet().add(merchantKey, authCode.getCode());
        redis.expire(merchantKey, TTL);
    }

    @Override
    public Optional<AuthCode> findByCode(String code) {
        String key = KEY_PREFIX + code;
        Map<Object, Object> entries = redis.opsForHash().entries(key);
        if (entries == null || entries.isEmpty()) {
            return Optional.empty();
        }
        AuthCode authCode = new AuthCode(
                (String) entries.get("code"),
                (String) entries.get("cardToken"),
                (String) entries.get("merchantId"),
                (String) entries.get("ownerId"),
                (String) entries.get("maskedNumber"),
                LocalDateTime.parse((String) entries.get("expiresAt"))
        );
        if ("true".equals(entries.get("used"))) {
            authCode.markAsUsed();
        }
        return Optional.of(authCode);
    }

    @Override
    public void deleteByMerchantId(String merchantId) {
        String merchantKey = MERCHANT_PREFIX + merchantId;
        Set<String> codes = redis.opsForSet().members(merchantKey);
        if (codes != null) {
            for (String code : codes) {
                redis.delete(KEY_PREFIX + code);
            }
        }
        redis.delete(merchantKey);
    }
}
