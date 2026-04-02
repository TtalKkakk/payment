package com.example.pg.payment.infrastructure.lock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

/**
 * 결제 승인/환불 비동기 처리의 중복 실행 방지(인스턴스 간 포함).
 * Redis SET NX + TTL(lease) 후 Lua로 토큰 일치 시에만 해제한다.
 */
@Slf4j
@Component
public class PaymentProcessDistributedLock {

    private static final String KEY_AUTH = "pg:lock:payment:authorize:";
    private static final String KEY_REFUND = "pg:lock:payment:refund:";

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>();
    static {
        UNLOCK_SCRIPT.setScriptText("if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) else return 0 end");
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    private final StringRedisTemplate redisTemplate;
    private final boolean enabled;
    private final int ttlSeconds;

    public PaymentProcessDistributedLock(
            StringRedisTemplate redisTemplate,
            @Value("${app.payment.process-lock.enabled:true}") boolean enabled,
            @Value("${app.payment.process-lock.ttl-seconds:180}") int ttlSeconds) {
        this.redisTemplate = redisTemplate;
        this.enabled = enabled;
        this.ttlSeconds = ttlSeconds;
    }

    public void runWithAuthorizeLock(String paymentId, Runnable action) {
        runWithLock(KEY_AUTH + paymentId, "authorize", paymentId, action);
    }

    public void runWithRefundLock(String paymentId, Runnable action) {
        runWithLock(KEY_REFUND + paymentId, "refund", paymentId, action);
    }

    private void runWithLock(String redisKey, String operation, String paymentId, Runnable action) {
        if (!enabled) {
            action.run();
            return;
        }
        String token = UUID.randomUUID().toString();
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, token, Duration.ofSeconds(ttlSeconds));
        if (!Boolean.TRUE.equals(locked)) {
            log.warn("[Payment] skip duplicate {} worker (lock held) paymentId={}", operation, paymentId);
            return;
        }
        try {
            action.run();
        } finally {
            tryUnlock(redisKey, token);
        }
    }

    private void tryUnlock(String redisKey, String token) {
        try {
            redisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(redisKey), token);
        } catch (Exception e) {
            log.warn("[Payment] lock release failed key={}", redisKey, e);
        }
    }
}
