package com.example.pg.common.cache;

import java.util.Optional;

/**
 * 카드 등록 플로우용 세션 저장소.
 * 카드사 선택 후 리다이렉트 전에 token을 발급하고, 콜백에서 token으로 cardCompanyCode·returnUrl을 복원한다.
 */
public interface SessionStore<T> {

    /**
     * 세션 저장 후 토큰 반환.
     */
    String put(T cache, String prefix, long ttl);

    Optional<T> get(String key, Class<T> type);

    void remove(String key);
}
