package com.example.pg.card_company.application;

import com.example.pg.card_company.presentation.dto.BillingKeyTokenResponse;
import com.example.pg.card_company.util.CardCompanyPortRegistry;
import com.example.pg.card_company.infrastructure.persistence.CardCompanyRepository;
import com.example.pg.card_company.application.dto.CardRegisterSession;
import com.example.pg.card_company.presentation.CardCompanyConnect;
import com.example.pg.common.cache.SessionStore;
import com.example.pg.common.exception.BusinessException;
import com.example.pg.common.exception.ErrorCode;
import com.example.pg.card_company.domain.aggergate.CardCompany;
import com.example.pg.card_company.domain.enumerate.CardCompanyStatus;
import com.example.pg.card_company.domain.vo.CardCompanyCode;
import com.example.pg.card_company.presentation.dto.RegistrationSessionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardCompanyService {
    private final CardCompanyRepository cardCompanyRepository;
    private final SessionStore<CardRegisterSession> cardRegisterSessionSessionStore;
    private final CardCompanyPortRegistry cardCompanyPortRegistry;
    private static final String SESSION_PREFIX = "cardRegisterSession:";
    private static final long ttlForCardRegister = 900;

    @Transactional(readOnly = true)
    public List<CardCompany> findAllActive() {
        log.debug("[CardCompany] findAllActive cardCompanies");
        return cardCompanyRepository.findByStatusOrderByDisplayOrderAsc(CardCompanyStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public CardCompany findByCode(String code) {
        log.debug("[CardCompany] Query findByCode cardCompanyCode={}", code);
        return cardCompanyRepository.findByCode(new CardCompanyCode(code))
                .orElseThrow(() -> new BusinessException(ErrorCode.CARD_COMPANY_NOT_FOUND, code));
    }



    /**
     * 전략 패턴을 이용하여 카드사와 통신
     */
    public RegistrationSessionResponse createRegistrationSession(String cardCompanyCode, String returnUrl) {
        log.debug("[CardCompany] createRegistrationSession cardCompanyCode={}", cardCompanyCode);
        CardCompanyConnect port = cardCompanyPortRegistry.getPortOrThrow(cardCompanyCode);
        return port.createRegistrationSession(returnUrl);
    }

    public BillingKeyTokenResponse issueBillingKey(String cardCompanyCode, String authCode) {
        log.debug("[CardCompany] issueBillingKey cardCompanyCode={}", cardCompanyCode);
        CardCompanyConnect port = cardCompanyPortRegistry.getPortOrThrow(cardCompanyCode);
        return port.issueBillingKey(authCode);
    }





    /**
     * sessionStore(Redis) CRUD
     */
    public String saveCardRegisterSession(CardRegisterSession session){
        log.debug("[CardCompany] save CardRegisterSession {}, {}, {} in sessionStore",
                session.cardCompanyCode(),
                session.returnUrl(),
                session.merchantId());
        return cardRegisterSessionSessionStore.put(session, SESSION_PREFIX, ttlForCardRegister);
    }

    public CardRegisterSession getCardRegisterSession(String sessionToken){
        log.debug("[CardCompany] get CardRegisterSession key = {} in sessionStore", sessionToken);
        return cardRegisterSessionSessionStore.get(SESSION_PREFIX + sessionToken, CardRegisterSession.class)
                .orElseThrow(() -> new BusinessException(ErrorCode.CARD_REGISTER_SESSION_INVALID, sessionToken));
    }

    public void removeCardRegisterSession(String sessionToken){
        log.debug("[CardCompany] remove CardRegisterSession key = {} in sessionStore", sessionToken);
        cardRegisterSessionSessionStore.remove(SESSION_PREFIX + sessionToken);
    }
}
