package com.example.pg.cardcompany.domain.result;

/**
 * 카드사 등록 세션 생성 응답 (가이드 3.1).
 * PG는 token + registrationUrl을 사용해 사용자를 카드 등록 페이지로 보낸다.
 */
public record RegistrationSessionResult(
        String token,
        String registrationUrl
) {}
