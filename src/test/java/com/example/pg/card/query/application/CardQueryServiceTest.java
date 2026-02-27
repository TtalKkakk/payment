package com.example.pg.card.query.application;

import com.example.pg.card.domain.aggregate.CardRegistrationToken;
import com.example.pg.card.domain.repository.CardRegistrationTokenRepository;
import com.example.pg.exception.BusinessException;
import com.example.pg.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardQueryServiceTest {

    private static final String TOKEN_VAL = "token-abc";
    private static final String MERCHANT_ID = "merchant-1";
    private static final String OWNER_ID = "owner-1";
    private static final String RETURN_URL = "https://shop.example.com/callback";

    @Mock
    private CardRegistrationTokenRepository tokenRepository;

    @InjectMocks
    private CardQueryService cardQueryService;

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

    @Nested
    @DisplayName("validateToken")
    class ValidateToken {

        @Test
        @DisplayName("유효한 토큰이면 예외 없이 통과")
        void success() {
            when(tokenRepository.findById(TOKEN_VAL)).thenReturn(Optional.of(validToken()));

            assertThatCode(() -> cardQueryService.validateToken(TOKEN_VAL)).doesNotThrowAnyException();
            verify(tokenRepository).findById(TOKEN_VAL);
        }

        @Test
        @DisplayName("토큰 없으면 CARD_REGISTRATION_TOKEN_INVALID")
        void whenTokenNotFound() {
            when(tokenRepository.findById("invalid")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cardQueryService.validateToken("invalid"))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.CARD_REGISTRATION_TOKEN_INVALID);
        }

        @Test
        @DisplayName("토큰 만료되었으면 CARD_REGISTRATION_TOKEN_EXPIRED")
        void whenTokenExpired() {
            when(tokenRepository.findById(TOKEN_VAL)).thenReturn(Optional.of(expiredToken()));

            assertThatThrownBy(() -> cardQueryService.validateToken(TOKEN_VAL))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.CARD_REGISTRATION_TOKEN_EXPIRED);
        }
    }
}
