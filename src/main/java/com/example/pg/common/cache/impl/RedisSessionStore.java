package com.example.pg.common.cache.impl;

import com.example.pg.common.cache.SessionStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
        if (cache == null) {
            throw new IllegalArgumentException("cache must not be null");
        }
        if (prefix == null || prefix.isBlank()) {
            throw new IllegalArgumentException("prefix must not be null or blank");
        }
        String id = UUID.randomUUID().toString();
        String key = prefix + id;
        ttl = defenceTTLIfNegative(ttl);
        try {
            String json = objectMapper.writeValueAsString(cache);
            // set(key, value, long) 단독은 SETRANGE(offset) — ttl(초)를 넘기면 offset으로 오인되어 앞이 널 패딩됨
            redisTemplate.opsForValue().set(key, json, ttl, TimeUnit.SECONDS);
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
            log.warn("[Util] 역직렬화 실패 token={}", key, e);
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

