package com.example.pg.card.infrastructure.persistence;

import com.example.pg.card.domain.aggregate.CardRegistrationToken;
import com.example.pg.card.domain.repository.CardRegistrationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 카드 등록 토큰을 Redis에 저장.
 * TTL 10분. 카드 등록 완료 시 또는 가맹점 삭제 시 삭제.
 */
@Repository
@RequiredArgsConstructor
public class RedisCardRegistrationTokenRepository implements CardRegistrationTokenRepository {

    private static final String KEY_PREFIX = "card_registration_token:";
    private static final String IDX_PREFIX = "card_registration_token:idx:";
    private static final String MERCHANT_PREFIX = "card_registration_token:merchant:";
    private static final Duration TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redis;

    @Override
    public void save(CardRegistrationToken token) {
        String key = KEY_PREFIX + token.getToken();
        redis.opsForHash().put(key, "token", token.getToken());
        redis.opsForHash().put(key, "merchantId", token.getMerchantId());
        redis.opsForHash().put(key, "ownerId", token.getOwnerId());
        redis.opsForHash().put(key, "returnUrl", token.getReturnUrl());
        redis.opsForHash().put(key, "createdAt", token.getCreatedAt().toString());
        redis.expire(key, TTL);

        String idxKey = IDX_PREFIX + token.getMerchantId() + ":" + token.getOwnerId();
        redis.opsForValue().set(idxKey, token.getToken(), TTL);

        String merchantKey = MERCHANT_PREFIX + token.getMerchantId();
        redis.opsForSet().add(merchantKey, token.getToken());
        redis.expire(merchantKey, TTL);
    }

    @Override
    public Optional<CardRegistrationToken> findById(String token) {
        String key = KEY_PREFIX + token;
        List<Object> values = redis.opsForHash().multiGet(key, List.of("token", "merchantId", "ownerId", "returnUrl", "createdAt"));
        if (values == null || values.stream().anyMatch(v -> v == null || v.toString().isEmpty())) {
            return Optional.empty();
        }
        CardRegistrationToken t = new CardRegistrationToken(
                values.get(0).toString(),
                values.get(1).toString(),
                values.get(2).toString(),
                values.get(3).toString()
        );
        return Optional.of(t);
    }

    @Override
    public Optional<CardRegistrationToken> findByMerchantIdAndOwnerId(String merchantId, String ownerId) {
        String idxKey = IDX_PREFIX + merchantId + ":" + ownerId;
        String token = redis.opsForValue().get(idxKey);
        if (token == null || token.isEmpty()) {
            return Optional.empty();
        }
        return findById(token);
    }

    @Override
    public void delete(CardRegistrationToken token) {
        String key = KEY_PREFIX + token.getToken();
        redis.delete(key);
        String idxKey = IDX_PREFIX + token.getMerchantId() + ":" + token.getOwnerId();
        redis.delete(idxKey);
        String merchantKey = MERCHANT_PREFIX + token.getMerchantId();
        redis.opsForSet().remove(merchantKey, token.getToken());
    }

    @Override
    public void deleteByMerchantId(String merchantId) {
        String merchantKey = MERCHANT_PREFIX + merchantId;
        Set<String> tokens = redis.opsForSet().members(merchantKey);
        if (tokens != null) {
            for (String tokenVal : tokens) {
                findById(tokenVal).ifPresent(this::delete);
            }
        }
        redis.delete(merchantKey);
    }
}
