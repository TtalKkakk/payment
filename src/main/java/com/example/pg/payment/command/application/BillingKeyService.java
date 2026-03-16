package com.example.pg.payment.command.application;

import com.example.pg.payment.domain.repository.CardCompanyPortRegistry;
import com.example.pg.payment.presentation.CardCompanyConnect;
import com.example.pg.payment.presentation.dto.BillingKeyTokenResponse;
import com.example.pg.payment.presentation.dto.RegistrationSessionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 카드사 빌링키 연동 유스케이스.
 * 가이드라인 흐름: 등록 세션 생성 → (사용자 카드 등록) → authCode로 빌링키 발급.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BillingKeyService {

    private final CardCompanyPortRegistry portRegistry;

    /**
     * 1) 등록 세션 생성. PG는 반환된 token + registrationUrl로 사용자를 카드사 등록 페이지로 보낸다.
     * 전체 이동 URL = {카드사 baseUrl}{registrationUrl}
     */
    public RegistrationSessionResponse createRegistrationSession(String cardCompanyCode, String returnUrl) {
        log.debug("[Payment] createRegistrationSession cardCompanyCode={}", cardCompanyCode);
        CardCompanyConnect port = portRegistry.getPortOrThrow(cardCompanyCode);
        return port.createRegistrationSession(returnUrl);
    }

    /**
     * 4) authCode로 빌링키 발급. returnUrl 콜백에서 받은 authCode를 서버에서 한 번만 호출.
     */
    public BillingKeyTokenResponse issueBillingKey(String cardCompanyCode, String authCode) {
        log.debug("[Payment] issueBillingKey cardCompanyCode={}", cardCompanyCode);
        CardCompanyConnect port = portRegistry.getPortOrThrow(cardCompanyCode);
        return port.issueBillingKey(authCode);
    }
}
