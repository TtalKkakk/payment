package com.example.pg.payment.command.application;

import com.example.pg.common.exception.BusinessException;
import com.example.pg.common.exception.ErrorCode;
import com.example.pg.payment.command.application.dto.ExchangedDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * billingKeyToken을 브라우저에 노출하지 않기 위한 교환(code) 서비스.
 *
 * - PG 콜백에서 billingKeyToken을 Redis에 저장하고, 브라우저에는 code만 리다이렉트로 전달한다.
 * - 가맹점 서버는 /api/billing-keys/exchange 로 code를 보내 billingKeyToken을 서버-서버로 교환한다.
 */
@Service
@RequiredArgsConstructor
public class BillingKeyExchangeService {

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(5);
    private static final String KEY_PREFIX = "billingKeyExchange:";

    private final StringRedisTemplate redisTemplate;

    public String issueCode(
            String merchantId,
            String billingKeyToken,
            String cardCompanyCode,
            String cardBrand,
            String cardNumberMasked,
            String expiryMasked
    ) {
        String code = UUID.randomUUID().toString();
        String key = KEY_PREFIX + code;
        // 값 포맷: merchantId|cardCompanyCode|billingKeyToken|cardBrand|cardNumberMasked|expiryMasked
        // null은 Redis 값 포맷 충돌을 피하기 위해 빈 문자열로 저장한다.
        String value = merchantId + "|" +
                cardCompanyCode + "|" +
                billingKeyToken + "|" +
                (cardBrand != null ? cardBrand : "") + "|" +
                (cardNumberMasked != null ? cardNumberMasked : "") + "|" +
                (expiryMasked != null ? expiryMasked : "");
        redisTemplate.opsForValue().set(key, value, DEFAULT_TTL);
        return code;
    }

    public ExchangedDto exchangeOrThrow(String merchantId, String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException(ErrorCode.BILLING_KEY_EXCHANGE_CODE_INVALID);
        }
        String key = KEY_PREFIX + code;
        String value = redisTemplate.opsForValue().get(key);
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.BILLING_KEY_EXCHANGE_CODE_INVALID);
        }
        // 1회성: 먼저 삭제 (동시 요청에서도 한 번만 성공하도록)
        Boolean deleted = redisTemplate.delete(key);
        if (deleted == null || !deleted) {
            throw new BusinessException(ErrorCode.BILLING_KEY_EXCHANGE_CODE_INVALID);
        }

        // 레거시 호환:
        // - 과거: merchantId|cardCompanyCode|billingKeyToken (3필드)
        // - 현재: merchantId|cardCompanyCode|billingKeyToken|cardBrand|cardNumberMasked|expiryMasked (6필드)
        String[] parts = value.split("\\|", 6);
        if (parts.length != 3 && parts.length != 6) {
            throw new BusinessException(ErrorCode.BILLING_KEY_EXCHANGE_CODE_INVALID);
        }

        String storedMerchantId = parts[0];
        String cardCompanyCode = parts[1];
        String billingKeyToken = parts[2];
        String cardBrand = parts.length >= 4 ? parts[3] : null;
        String cardNumberMasked = parts.length >= 5 ? parts[4] : null;
        String expiryMasked = parts.length >= 6 ? parts[5] : null;

        if (!merchantId.equals(storedMerchantId)) {
            throw new BusinessException(ErrorCode.BILLING_KEY_EXCHANGE_CODE_INVALID);
        }

        return new ExchangedDto(
                billingKeyToken,
                cardCompanyCode,
                cardBrand,
                cardNumberMasked,
                expiryMasked
        );
    }
}

