package com.example.pg.cardcompany.application;

import com.example.pg.cardcompany.domain.port.CardCompanyBillingKeyPort;
import com.example.pg.cardcompany.domain.result.BillingKeyTokenResult;
import com.example.pg.cardcompany.domain.result.RegistrationSessionResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 카드사 빌링키 연동 유스케이스.
 * 가이드라인 흐름: 등록 세션 생성 → (사용자 카드 등록) → authCode로 빌링키 발급.
 */
@Service
@RequiredArgsConstructor
public class CardCompanyBillingKeyService {

    private final CardCompanyPortRegistry portRegistry;

    /**
     * 1) 등록 세션 생성. PG는 반환된 token + registrationUrl로 사용자를 카드사 등록 페이지로 보낸다.
     * 전체 이동 URL = {카드사 baseUrl}{registrationUrl}
     */
    public RegistrationSessionResult createRegistrationSession(String cardCompanyCode, String returnUrl) {
        CardCompanyBillingKeyPort port = portRegistry.getPortOrThrow(cardCompanyCode);
        return port.createRegistrationSession(returnUrl);
    }

    /**
     * 4) authCode로 빌링키 발급. returnUrl 콜백에서 받은 authCode를 서버에서 한 번만 호출.
     */
    public BillingKeyTokenResult issueBillingKey(String cardCompanyCode, String authCode) {
        CardCompanyBillingKeyPort port = portRegistry.getPortOrThrow(cardCompanyCode);
        return port.issueBillingKey(authCode);
    }
}
