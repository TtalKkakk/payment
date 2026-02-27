package com.example.pg.card.command.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.pg.exception.BusinessException;
import com.example.pg.exception.ErrorCode;
import com.example.pg.card.command.application.result.BillingKeyExchangeResult;
import com.example.pg.card.command.application.result.CreateSessionResult;
import com.example.pg.card.domain.aggregate.AuthCode;
import com.example.pg.card.domain.aggregate.Card;
import com.example.pg.card.domain.aggregate.CardRegistrationToken;
import com.example.pg.card.domain.event.BillingKeyIssuedEvent;
import com.example.pg.card.domain.event.CardRegisteredEvent;
import com.example.pg.card.domain.repository.AuthCodeRepository;
import com.example.pg.card.domain.repository.CardCommandRepository;
import com.example.pg.card.domain.repository.CardRegistrationTokenRepository;
import com.example.pg.card.domain.vo.CardToken;
import com.example.pg.card.command.application.port.BillingKeyPort;
import com.example.pg.card.command.application.result.CardRegistrationResult;
import com.example.pg.merchant.query.application.MerchantQueryService;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 카드 도메인 명령 전용 (CQRS - Command).
 * 세션 생성, 카드 등록, 빌링키 교환 등 상태를 변경하는 작업만 수행한다.
 * 조회·검증은 {@link com.example.pg.card.query.application.CardQueryService}를 사용한다.
 */
@Service
@RequiredArgsConstructor
public class CardService {

    private static final int AUTH_CODE_EXPIRY_MINUTES = 5;

    private final CardCommandRepository cardCommandRepository;
    private final CardRegistrationTokenRepository tokenRepository;
    private final AuthCodeRepository authCodeRepository;
    private final BillingKeyPort billingKeyPort;
    private final ApplicationEventPublisher eventPublisher;
    private final MerchantQueryService merchantQueryService;

    @Transactional
    public CreateSessionResult createToken(String merchantId, String ownerId, String returnUrl) {
        merchantQueryService.findById(merchantId);

        return tokenRepository.findByMerchantIdAndOwnerId(merchantId, ownerId)
                .map(existing -> {
                    existing.updateReturnUrl(returnUrl);
                    tokenRepository.save(existing);
                    String tokenVal = existing.getToken();
                    String registrationUrl = "/card/register?token=" + tokenVal;
                    return new CreateSessionResult(tokenVal, registrationUrl);
                })
                .orElseGet(() -> {
                    String tokenVal = UUID.randomUUID().toString();
                    CardRegistrationToken tokenEntity = new CardRegistrationToken(tokenVal, merchantId, ownerId, returnUrl);
                    tokenRepository.save(tokenEntity);
                    String registrationUrl = "/card/register?token=" + tokenVal;
                    return new CreateSessionResult(tokenVal, registrationUrl);
                });
    }

    @Transactional
    public CardRegistrationResult registerCard(String token, String cardNumber, String expiry, String cvc) {
        CardRegistrationToken tokenEntity = tokenRepository.findById(token)
                .orElseThrow(() -> new BusinessException(ErrorCode.CARD_REGISTRATION_TOKEN_INVALID, token));
        if (tokenEntity.isExpired()) {
            throw new BusinessException(ErrorCode.CARD_REGISTRATION_TOKEN_EXPIRED);
        }
        validateExpiry(expiry);

        // 아주 단순한 마스킹 처리 (예시용)
        String last4 = cardNumber.length() >= 4 ? cardNumber.substring(cardNumber.length() - 4) : cardNumber;
        String maskedNumber = "****-****-****-" + last4;

        CardToken cardToken = CardToken.generate();
        Card card = new Card(cardToken, tokenEntity.getOwnerId(), maskedNumber, null);
        cardCommandRepository.save(card);

        String authCode = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(AUTH_CODE_EXPIRY_MINUTES);
        AuthCode authCodeEntity = new AuthCode(
                authCode,
                cardToken.getValue(),
                tokenEntity.getMerchantId(),
                tokenEntity.getOwnerId(),
                maskedNumber,
                expiresAt
        );
        authCodeRepository.save(authCodeEntity);

        tokenRepository.delete(tokenEntity);

        eventPublisher.publishEvent(CardRegisteredEvent.from(tokenEntity.getMerchantId(), tokenEntity.getOwnerId(), maskedNumber));

        return new CardRegistrationResult(authCode, tokenEntity.getReturnUrl());
    }

    /**
     * 1회용 authCode를 빌링키로 교환한다.
     * authCode 검증 후 카드사/은행 포트로 빌링키 발급 요청하여 반환한다.
     * 가맹점 인증 필수. authCode는 해당 가맹점의 세션에서 발급된 것만 유효하다.
     */
    @Transactional
    public BillingKeyExchangeResult exchangeBillingKey(String merchantId, String authCode) {
        AuthCode authCodeEntity = authCodeRepository.findByCode(authCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_CODE_INVALID));

        if (!authCodeEntity.isUsable()) {
            throw new BusinessException(ErrorCode.AUTH_CODE_EXPIRED_OR_USED);
        }

        if (!authCodeEntity.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ErrorCode.AUTH_CODE_UNAUTHORIZED);
        }

        String billingKey = billingKeyPort.issueBillingKey(authCodeEntity.getCardToken());
        authCodeEntity.markAsUsed();
        authCodeRepository.save(authCodeEntity);

        eventPublisher.publishEvent(BillingKeyIssuedEvent.from(
                authCodeEntity.getMerchantId(), authCodeEntity.getOwnerId(), authCodeEntity.getMaskedNumber()));

        return new BillingKeyExchangeResult(
                billingKey,
                authCodeEntity.getOwnerId(),
                authCodeEntity.getMaskedNumber()
        );
    }

    /**
     * 유효기간(MM/YY) 형식 및 월(01~12) 검증
     */
    private void validateExpiry(String expiry) {
        if (expiry == null || !expiry.matches("\\d{2}/\\d{2}")) {
            throw new BusinessException(ErrorCode.EXPIRY_FORMAT_INVALID);
        }
        int month = Integer.parseInt(expiry.substring(0, 2));
        if (month < 1 || month > 12) {
            throw new BusinessException(ErrorCode.EXPIRY_MONTH_INVALID);
        }
    }
}
