package com.example.pg.payment.infrastructure.persistence;

import com.example.pg.payment.domain.repository.dto.CardRegisterSession;

import java.util.Optional;

/**
 * 카드 등록 플로우용 세션 저장소.
 * 카드사 선택 후 리다이렉트 전에 token을 발급하고, 콜백에서 token으로 cardCompanyCode·returnUrl을 복원한다.
 */
public interface CardRegisterSessionStore {

    /**
     * 세션 저장 후 토큰 반환.
     */
    String put(String cardCompanyCode, String returnUrl);

    Optional<CardRegisterSession> get(String token);

    void remove(String token);
}
