package com.example.pg.payment.domain.repository;

import com.example.pg.payment.domain.repository.dto.CardRegisterSession;
import com.example.pg.payment.infrastructure.persistence.CardRegisterSessionStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InMemoryCardRegisterSessionStore")
class InMemoryCardRegisterSessionStoreTest {

    private CardRegisterSessionStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryCardRegisterSessionStore();
    }

    @Nested
    @DisplayName("put / get")
    class PutAndGet {

        @Test
        @DisplayName("put 후 get으로 동일 토큰으로 조회 가능")
        void success() {
            String token = store.put("SHINHAN", "https://merchant.com/callback");

            assertThat(token).isNotBlank();

            Optional<CardRegisterSession> session = store.get(token);
            assertThat(session).isPresent();
            assertThat(session.get().cardCompanyCode()).isEqualTo("SHINHAN");
            assertThat(session.get().returnUrl()).isEqualTo("https://merchant.com/callback");
        }

        @Test
        @DisplayName("없는 토큰으로 get 시 empty")
        void unknownToken() {
            assertThat(store.get("unknown-token")).isEmpty();
        }
    }

    @Nested
    @DisplayName("remove")
    class Remove {

        @Test
        @DisplayName("remove 후 get 시 empty")
        void success() {
            String token = store.put("KB", "https://cb.com");
            store.remove(token);

            assertThat(store.get(token)).isEmpty();
        }
    }
}
