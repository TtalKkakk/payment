package com.example.pg.payment.domain.repository;

import com.example.pg.payment.domain.repository.dto.CardRegisterSession;
import com.example.pg.payment.infrastructure.persistence.CardRegisterSessionStore;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@org.springframework.context.annotation.Profile("inmemory-session-store")
@Component
public class InMemoryCardRegisterSessionStore implements CardRegisterSessionStore {

    private final ConcurrentHashMap<String, CardRegisterSession> store = new ConcurrentHashMap<>();

    @Override
    public String put(String cardCompanyCode, String returnUrl, String merchantId) {
        String token = UUID.randomUUID().toString();
        store.put(token, new CardRegisterSession(cardCompanyCode, returnUrl, merchantId));
        return token;
    }

    @Override
    public Optional<CardRegisterSession> get(String token) {
        return Optional.ofNullable(store.get(token));
    }

    @Override
    public void remove(String token) {
        store.remove(token);
    }
}
