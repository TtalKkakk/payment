package com.example.pg.payment.domain.repository;

import com.example.pg.payment.infrastructure.persistence.CardCompanyRepository;
import com.example.pg.common.exception.BusinessException;
import com.example.pg.common.exception.ErrorCode;
import com.example.pg.payment.presentation.CardCompanyConnect;
import com.example.pg.payment.presentation.impl.CardCompanyApiTemplate;
import com.example.pg.payment.domain.aggregate.CardCompany;
import com.example.pg.payment.domain.enumerate.CardCompanyStatus;
import com.example.pg.payment.presentation.impl.CardCompanyAConnectImpl;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 카드사 코드별 빌링키 포트(전략) 레지스트리.
 * ACTIVE 카드사마다 HttpCardCompanyAdapter 인스턴스를 생성해 등록한다.
 * 카드사 추가 시 재기동 후 반영되며, 필요 시 refresh API로 갱신 가능.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CardCompanyPortRegistry {

    private final CardCompanyRepository cardCompanyRepository;
    private final CardCompanyApiTemplate apiTemplate;
    private final Map<String, CardCompanyConnect> portByCode = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        refresh();
    }

    public void refresh() {
        portByCode.clear();
        List<CardCompany> companies = cardCompanyRepository.findByStatusOrderByDisplayOrderAsc(CardCompanyStatus.ACTIVE);
        companies.stream()
                .filter(c -> c.getBaseUrl() != null && !c.getBaseUrl().isBlank())
                .forEach(c -> {
                    CardCompanyConnect adapter = new CardCompanyAConnectImpl(
                            apiTemplate,
                            c.getBaseUrl()
                    );
                    portByCode.put(c.getCode(), adapter);
                    log.debug("[Payment] CardCompany port registered code={} baseUrl={}", c.getCode(), c.getBaseUrl());
                });
    }

    public Optional<CardCompanyConnect> getPort(String cardCompanyCode) {
        return Optional.ofNullable(portByCode.get(cardCompanyCode));
    }

    public CardCompanyConnect getPortOrThrow(String cardCompanyCode) {
        return getPort(cardCompanyCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.CARD_COMPANY_NOT_FOUND, cardCompanyCode));
    }
}
