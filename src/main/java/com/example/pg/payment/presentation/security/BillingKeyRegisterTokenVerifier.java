package com.example.pg.payment.presentation.security;

import com.example.pg.common.exception.BusinessException;
import com.example.pg.common.exception.ErrorCode;
import com.example.pg.merchant.domain.aggregate.Merchant;
import com.example.pg.merchant.infrastructure.persistence.MerchantRepository;
import com.example.pg.payment.presentation.dto.BillingKeyRegisterTokenPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * /billing-key/register 접근 토큰 검증기.
 *
 * 토큰 포맷: base64url(payloadJson) + "." + base64url(HMAC_SHA256(apiSecret, base64url(payloadJson)))
 */
@Component
@RequiredArgsConstructor
public class BillingKeyRegisterTokenVerifier {

    public static final String ATTR_MERCHANT_ID = "billingKeyRegister.merchantId";
    public static final String ATTR_RETURN_URL = "billingKeyRegister.returnUrl";
    public static final String ATTR_API_KEY = "billingKeyRegister.apiKey";

    private static final String PURPOSE = "billing_key_register";
    private static final String HMAC_SHA256 = "HmacSHA256";

    private final ObjectMapper objectMapper;
    private final MerchantRepository merchantRepository;
    private final StringRedisTemplate redisTemplate;

    public Verified verifyOrThrow(String token) {
        return verifyOrThrow(token, true);
    }

    /**
     * @param consumeNonce true면 nonce를 1회성으로 소비한다.
     *                     false면 nonce 소비를 스킵한다(예: GET 페이지 렌더링 단계).
     */
    public Verified verifyOrThrow(String token, boolean consumeNonce) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(ErrorCode.BILLING_KEY_REGISTER_TOKEN_INVALID);
        }

        String[] parts = token.split("\\.");
        if (parts.length != 2) {
            throw new BusinessException(ErrorCode.BILLING_KEY_REGISTER_TOKEN_INVALID);
        }
        String payloadB64 = parts[0];
        String sigB64 = parts[1];

        BillingKeyRegisterTokenPayload payload = decodePayload(payloadB64);

        if (payload.apiKey() == null || payload.apiKey().isBlank()) {
            throw new BusinessException(ErrorCode.BILLING_KEY_REGISTER_TOKEN_INVALID);
        }
        if (payload.nonce() == null || payload.nonce().isBlank()) {
            throw new BusinessException(ErrorCode.BILLING_KEY_REGISTER_TOKEN_INVALID);
        }
        if (payload.purpose() == null || !PURPOSE.equals(payload.purpose())) {
            throw new BusinessException(ErrorCode.BILLING_KEY_REGISTER_TOKEN_INVALID);
        }

        long now = Instant.now().getEpochSecond();
        if (payload.exp() <= now) {
            throw new BusinessException(ErrorCode.BILLING_KEY_REGISTER_TOKEN_INVALID);
        }

        Merchant merchant = merchantRepository.findByApiKey(payload.apiKey())
                .orElseThrow(() -> new BusinessException(ErrorCode.BILLING_KEY_REGISTER_TOKEN_INVALID));

        String expectedSig = hmacBase64Url(merchant.getApiSecret(), payloadB64);
        if (!constantTimeEquals(expectedSig, sigB64)) {
            throw new BusinessException(ErrorCode.BILLING_KEY_REGISTER_TOKEN_INVALID);
        }

        if (consumeNonce) {
            // replay 방지: apiKey+nonce를 1회성으로 소비 처리 (exp까지 TTL)
            String nonceKey = "billingKeyRegister:nonce:" + payload.apiKey() + ":" + payload.nonce();
            Duration ttl = Duration.ofSeconds(Math.max(1, payload.exp() - now));
            Boolean ok = redisTemplate.opsForValue().setIfAbsent(nonceKey, "1", ttl);
            if (ok == null || !ok) {
                throw new BusinessException(ErrorCode.BILLING_KEY_REGISTER_TOKEN_INVALID);
            }
        }

        return new Verified(merchant.getId(), payload.apiKey(), payload.returnUrl());
    }

    private BillingKeyRegisterTokenPayload decodePayload(String payloadB64) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(payloadB64);
            return objectMapper.readValue(decoded, BillingKeyRegisterTokenPayload.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.BILLING_KEY_REGISTER_TOKEN_INVALID);
        }
    }

    private static String hmacBase64Url(String secret, String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.BILLING_KEY_REGISTER_TOKEN_INVALID);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        if (a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    public record Verified(
            String merchantId,
            String apiKey,
            String returnUrl
    ) {}
}

