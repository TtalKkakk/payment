package com.example.pg.cardcompany.domain.port;

import com.example.pg.cardcompany.domain.result.BillingKeyTokenResult;
import com.example.pg.cardcompany.domain.result.RegistrationSessionResult;

/**
 * 카드사 빌링키 연동 전략(Strategy) 포트.
 * 카드사마다 다른 구현체(어댑터)가 있으며, 동일 가이드라인(등록 세션 → authCode → 빌링키)에 맞춰 동작한다.
 */
public interface CardCompanyBillingKeyPort {

    /**
     * 등록 세션 생성. 카드사 API POST /card-registration-session 호출.
     *
     * @param returnUrl 사용자 카드 등록 완료 후 리다이렉트될 PG 콜백 URL (authCode가 쿼리로 붙음)
     * @return token, registrationUrl (상대 경로. PG가 baseUrl과 결합해 사용자 이동)
     */
    RegistrationSessionResult createRegistrationSession(String returnUrl);

    /**
     * authCode로 빌링키 발급. 카드사 API POST /billing-keys 호출.
     *
     * @param authCode returnUrl 쿼리로 전달된 1회용 인증 코드
     * @return 발급된 빌링키 토큰
     */
    BillingKeyTokenResult issueBillingKey(String authCode);
}
