package com.example.pg.card.command.application;

import com.example.pg.card.command.application.port.BillingKeyPort;
import com.example.pg.card.command.application.result.BillingKeyExchangeResult;
import com.example.pg.card.command.application.result.CardRegistrationResult;
import com.example.pg.card.command.application.result.CreateSessionResult;
import com.example.pg.card.domain.aggregate.AuthCode;
import com.example.pg.card.domain.aggregate.CardRegistrationToken;
import com.example.pg.card.domain.event.BillingKeyIssuedEvent;
import com.example.pg.card.domain.event.CardRegisteredEvent;
import com.example.pg.card.domain.repository.AuthCodeRepository;
import com.example.pg.card.domain.repository.CardCommandRepository;
import com.example.pg.card.domain.repository.CardRegistrationTokenRepository;
import com.example.pg.exception.BusinessException;
import com.example.pg.exception.ErrorCode;
import com.example.pg.merchant.query.application.MerchantQueryService;
import com.example.pg.merchant.domain.aggregate.Merchant;
import com.example.pg.merchant.domain.enumerate.MerchantStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    private static final String MERCHANT_ID = "merchant-1";
    private static final String OWNER_ID = "owner-1";
    private static final String RETURN_URL = "https://shop.example.com/callback";
    private static final String TOKEN_VAL = "token-abc";
    private static final String AUTH_CODE = "auth123";
    private static final String CARD_TOKEN = "ct_xyz";
    private static final String MASKED_NUMBER = "****-****-****-1234";

    @Mock
    private CardCommandRepository cardCommandRepository;
    @Mock
    private CardRegistrationTokenRepository tokenRepository;
    @Mock
    private AuthCodeRepository authCodeRepository;
    @Mock
    private BillingKeyPort billingKeyPort;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private MerchantQueryService merchantQueryService;

    @InjectMocks
    private CardService cardService;

    private static CardRegistrationToken validToken() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(10);
        return new CardRegistrationToken(TOKEN_VAL, MERCHANT_ID, OWNER_ID, RETURN_URL, now, expiresAt);
    }

    private static CardRegistrationToken expiredToken() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.minusMinutes(1);
        return new CardRegistrationToken(TOKEN_VAL, MERCHANT_ID, OWNER_ID, RETURN_URL, now, expiresAt);
    }

    private static AuthCode usableAuthCode() {
        return new AuthCode(AUTH_CODE, CARD_TOKEN, MERCHANT_ID, OWNER_ID, MASKED_NUMBER,
                LocalDateTime.now().plusMinutes(5));
    }

    @Nested
    @DisplayName("createToken")
    class CreateToken {

        @Test
        @DisplayName("기존 토큰 있으면 returnUrl 갱신 후 기존 토큰·URL 반환")
        void whenExistingToken() {
            Merchant merchant = new Merchant(MERCHANT_ID, "pk", "sk", "name", "app-1", MerchantStatus.ACTIVE);
            when(merchantQueryService.findById(MERCHANT_ID)).thenReturn(merchant);
            CardRegistrationToken existing = validToken();
            when(tokenRepository.findByMerchantIdAndOwnerId(MERCHANT_ID, OWNER_ID)).thenReturn(Optional.of(existing));

            CreateSessionResult result = cardService.createToken(MERCHANT_ID, OWNER_ID, RETURN_URL);

            assertThat(result.token()).isEqualTo(TOKEN_VAL);
            assertThat(result.registrationUrl()).isEqualTo("/card/register?token=" + TOKEN_VAL);
            verify(tokenRepository).save(existing);
        }

        @Test
        @DisplayName("기존 토큰 없으면 새 토큰 생성 후 저장·반환")
        void whenNoExistingToken() {
            Merchant merchant = new Merchant(MERCHANT_ID, "pk", "sk", "name", "app-1", MerchantStatus.ACTIVE);
            when(merchantQueryService.findById(MERCHANT_ID)).thenReturn(merchant);
            when(tokenRepository.findByMerchantIdAndOwnerId(MERCHANT_ID, OWNER_ID)).thenReturn(Optional.empty());

            CreateSessionResult result = cardService.createToken(MERCHANT_ID, OWNER_ID, RETURN_URL);

            assertThat(result.token()).isNotNull();
            assertThat(result.registrationUrl()).isEqualTo("/card/register?token=" + result.token());
            ArgumentCaptor<CardRegistrationToken> captor = ArgumentCaptor.forClass(CardRegistrationToken.class);
            verify(tokenRepository).save(captor.capture());
            assertThat(captor.getValue().getMerchantId()).isEqualTo(MERCHANT_ID);
            assertThat(captor.getValue().getOwnerId()).isEqualTo(OWNER_ID);
            assertThat(captor.getValue().getReturnUrl()).isEqualTo(RETURN_URL);
        }
    }

    @Nested
    @DisplayName("registerCard")
    class RegisterCard {

        @Test
        @DisplayName("유효한 토큰이면 카드 저장·AuthCode 저장·토큰 삭제·이벤트 발행 후 authCode·returnUrl 반환")
        void success() {
            CardRegistrationToken tokenEntity = validToken();
            when(tokenRepository.findById(TOKEN_VAL)).thenReturn(Optional.of(tokenEntity));

            CardRegistrationResult result = cardService.registerCard(TOKEN_VAL, "1234567812345678", "12/28", "123");

            assertThat(result.authCode()).isNotNull();
            assertThat(result.returnUrl()).isEqualTo(RETURN_URL);
            verify(cardCommandRepository).save(any());
            verify(authCodeRepository).save(any(AuthCode.class));
            verify(tokenRepository).delete(tokenEntity);
            ArgumentCaptor<CardRegisteredEvent> captor = ArgumentCaptor.forClass(CardRegisteredEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().merchantId()).isEqualTo(MERCHANT_ID);
            assertThat(captor.getValue().ownerId()).isEqualTo(OWNER_ID);
        }

        @Test
        @DisplayName("토큰 없으면 CARD_REGISTRATION_TOKEN_INVALID")
        void whenTokenNotFound() {
            when(tokenRepository.findById("invalid")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cardService.registerCard("invalid", "1234567812345678", "12/28", "123"))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.CARD_REGISTRATION_TOKEN_INVALID);
            verify(cardCommandRepository, never()).save(any());
            verify(authCodeRepository, never()).save(any());
        }

        @Test
        @DisplayName("토큰 만료되었으면 CARD_REGISTRATION_TOKEN_EXPIRED")
        void whenTokenExpired() {
            when(tokenRepository.findById(TOKEN_VAL)).thenReturn(Optional.of(expiredToken()));

            assertThatThrownBy(() -> cardService.registerCard(TOKEN_VAL, "1234567812345678", "12/28", "123"))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.CARD_REGISTRATION_TOKEN_EXPIRED);
            verify(cardCommandRepository, never()).save(any());
            verify(authCodeRepository, never()).save(any());
        }

        @Test
        @DisplayName("유효기간 형식 잘못되면 EXPIRY_FORMAT_INVALID")
        void whenExpiryFormatInvalid() {
            when(tokenRepository.findById(TOKEN_VAL)).thenReturn(Optional.of(validToken()));

            assertThatThrownBy(() -> cardService.registerCard(TOKEN_VAL, "1234567812345678", "13/28", "123"))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.EXPIRY_FORMAT_INVALID);
            assertThatThrownBy(() -> cardService.registerCard(TOKEN_VAL, "1234567812345678", null, "123"))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.EXPIRY_FORMAT_INVALID);
        }

        @Test
        @DisplayName("월이 01~12 밖이면 EXPIRY_MONTH_INVALID")
        void whenExpiryMonthInvalid() {
            when(tokenRepository.findById(TOKEN_VAL)).thenReturn(Optional.of(validToken()));

            assertThatThrownBy(() -> cardService.registerCard(TOKEN_VAL, "1234567812345678", "00/28", "123"))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.EXPIRY_MONTH_INVALID);
            assertThatThrownBy(() -> cardService.registerCard(TOKEN_VAL, "1234567812345678", "13/28", "123"))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.EXPIRY_MONTH_INVALID);
        }
    }

    @Nested
    @DisplayName("exchangeBillingKey")
    class ExchangeBillingKey {

        @Test
        @DisplayName("유효한 authCode·merchantId면 빌링키 발급·사용 처리·이벤트 발행 후 결과 반환")
        void success() {
            AuthCode authCodeEntity = usableAuthCode();
            when(authCodeRepository.findByCode(AUTH_CODE)).thenReturn(Optional.of(authCodeEntity));
            when(billingKeyPort.issueBillingKey(CARD_TOKEN)).thenReturn("bk_issued");

            BillingKeyExchangeResult result = cardService.exchangeBillingKey(MERCHANT_ID, AUTH_CODE);

            assertThat(result.billingKey()).isEqualTo("bk_issued");
            assertThat(result.ownerId()).isEqualTo(OWNER_ID);
            assertThat(result.maskedNumber()).isEqualTo(MASKED_NUMBER);
            verify(authCodeRepository).save(authCodeEntity);
            assertThat(authCodeEntity.isUsed()).isTrue();
            verify(eventPublisher).publishEvent(any(BillingKeyIssuedEvent.class));
        }

        @Test
        @DisplayName("authCode 없으면 AUTH_CODE_INVALID")
        void whenAuthCodeNotFound() {
            when(authCodeRepository.findByCode("invalid")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cardService.exchangeBillingKey(MERCHANT_ID, "invalid"))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.AUTH_CODE_INVALID);
            verify(billingKeyPort, never()).issueBillingKey(any());
        }

        @Test
        @DisplayName("authCode 이미 사용됐으면 AUTH_CODE_EXPIRED_OR_USED")
        void whenAuthCodeUsed() {
            AuthCode authCodeEntity = usableAuthCode();
            authCodeEntity.markAsUsed();
            when(authCodeRepository.findByCode(AUTH_CODE)).thenReturn(Optional.of(authCodeEntity));

            assertThatThrownBy(() -> cardService.exchangeBillingKey(MERCHANT_ID, AUTH_CODE))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.AUTH_CODE_EXPIRED_OR_USED);
            verify(billingKeyPort, never()).issueBillingKey(any());
        }

        @Test
        @DisplayName("merchantId 불일치면 AUTH_CODE_UNAUTHORIZED")
        void whenMerchantIdMismatch() {
            AuthCode authCodeEntity = usableAuthCode();
            when(authCodeRepository.findByCode(AUTH_CODE)).thenReturn(Optional.of(authCodeEntity));

            assertThatThrownBy(() -> cardService.exchangeBillingKey("other-merchant", AUTH_CODE))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.AUTH_CODE_UNAUTHORIZED);
            verify(billingKeyPort, never()).issueBillingKey(any());
        }
    }
}
