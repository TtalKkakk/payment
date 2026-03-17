package com.example.pg.payment.infrastructure.persistence;

import com.example.pg.payment.domain.repository.dto.CardRegisterSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Redis 기반 카드 등록 세션 저장소.
 * token은 1회성으로 사용되며, TTL이 지나면 자동 만료된다.
 */
@Slf4j
@Component
public class RedisCardRegisterSessionStore implements CardRegisterSessionStore {

    private static final String KEY_PREFIX = "cardRegisterSession:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    public RedisCardRegisterSessionStore(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${app.payment.card-register-session-ttl-seconds:900}") long ttlSeconds
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.ttl = Duration.ofSeconds(Math.max(1, ttlSeconds));
    }

    @Override
    public String put(String cardCompanyCode, String returnUrl) {
        String token = UUID.randomUUID().toString();
        String key = KEY_PREFIX + token;
        try {
            String json = objectMapper.writeValueAsString(new CardRegisterSession(cardCompanyCode, returnUrl));
            redisTemplate.opsForValue().set(key, json, ttl);
            return token;
        } catch (Exception e) {
            log.error("[Payment] CardRegisterSession 저장 실패", e);
            throw new RuntimeException("카드 등록 세션 저장에 실패했습니다.", e);
        }
    }

    @Override
    public Optional<CardRegisterSession> get(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String key = KEY_PREFIX + token;
        String json = redisTemplate.opsForValue().get(key);
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, CardRegisterSession.class));
        } catch (Exception e) {
            log.warn("[Payment] CardRegisterSession 역직렬화 실패 token={}", token, e);
            return Optional.empty();
        }
    }

    @Override
    public void remove(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        redisTemplate.delete(KEY_PREFIX + token);
    }
}

