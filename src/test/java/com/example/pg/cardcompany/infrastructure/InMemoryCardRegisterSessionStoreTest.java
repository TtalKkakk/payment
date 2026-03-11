package com.example.pg.cardcompany.infrastructure;

import com.example.pg.cardcompany.application.CardRegisterSessionStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InMemoryCardRegisterSessionStore")
class InMemoryCardRegisterSessionStoreTest {

    private final InMemoryCardRegisterSessionStore sut = new InMemoryCardRegisterSessionStore();

    private static final String CARD_COMPANY_CODE = "CARD_A";
    private static final String RETURN_URL = "https://merchant.example.com/callback";

    @Nested
    @DisplayName("put / get")
    class PutAndGet {

        @Test
        @DisplayName("put 시 토큰 반환, get으로 동일 세션 복원")
        void success() {
            String token = sut.put(CARD_COMPANY_CODE, RETURN_URL);

            assertThat(token).isNotBlank();

            Optional<CardRegisterSessionStore.CardRegisterSession> session = sut.get(token);
            assertThat(session).isPresent();
            assertThat(session.get().cardCompanyCode()).isEqualTo(CARD_COMPANY_CODE);
            assertThat(session.get().returnUrl()).isEqualTo(RETURN_URL);
        }

        @Test
        @DisplayName("없는 토큰으로 get 시 empty")
        void getUnknownToken() {
            assertThat(sut.get("unknown-token")).isEmpty();
        }
    }

    @Nested
    @DisplayName("remove")
    class Remove {

        @Test
        @DisplayName("remove 후 get 시 empty")
        void success() {
            String token = sut.put(CARD_COMPANY_CODE, RETURN_URL);
            sut.remove(token);

            assertThat(sut.get(token)).isEmpty();
        }

        @Test
        @DisplayName("없는 토큰 remove 해도 예외 없음")
        void removeUnknownToken() {
            sut.remove("unknown-token");
        }
    }

    @Nested
    @DisplayName("토큰 고유성")
    class TokenUniqueness {

        @Test
        @DisplayName("put 두 번 호출 시 서로 다른 토큰 반환")
        void differentTokens() {
            String token1 = sut.put(CARD_COMPANY_CODE, RETURN_URL);
            String token2 = sut.put(CARD_COMPANY_CODE, RETURN_URL);

            assertThat(token1).isNotEqualTo(token2);
            assertThat(sut.get(token1)).isPresent();
            assertThat(sut.get(token2)).isPresent();
        }
    }
}
