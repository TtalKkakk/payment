package com.example.pg.common.cache.impl;

import com.example.pg.common.cache.SessionStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Redis 기반 카드 등록 세션 저장소.
 * token은 1회성으로 사용되며, TTL이 지나면 자동 만료된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisSessionStore<T> implements SessionStore<T> {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public String put(T cache, String prefix, long ttl) {
        String id = UUID.randomUUID().toString();
        String key = prefix + id;
        ttl = defenceTTLIfNegative(ttl);
        try {
            String json = objectMapper.writeValueAsString(cache);
            redisTemplate.opsForValue().set(key, json, ttl);
            return id;
        } catch (Exception e) {
            log.error("[Util] sessionStore 저장 실패", e);
            throw new RuntimeException("카드 등록 세션 저장에 실패했습니다.", e);
        }
    }

    @Override
    public Optional<T> get(String key, Class<T> type) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        String json = redisTemplate.opsForValue().get(key);
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, type));
        } catch (Exception e) {
            log.warn("[Payment] CardRegisterSession 역직렬화 실패 token={}", key, e);
            return Optional.empty();
        }
    }

    @Override
    public void remove(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        redisTemplate.delete(key);
    }

    private long defenceTTLIfNegative(long ttl){
        return Math.max(ttl, 1);
    }
}

