package com.example.pg.cardcompany.application;

import com.example.pg.cardcompany.domain.aggregate.CardCompany;
import com.example.pg.cardcompany.domain.enumerate.CardCompanyStatus;
import com.example.pg.cardcompany.domain.port.CardCompanyBillingKeyPort;
import com.example.pg.cardcompany.infrastructure.adapter.HttpCardCompanyBillingKeyAdapter;
import com.example.pg.cardcompany.infrastructure.persistence.CardCompanyRepository;
import com.example.pg.exception.BusinessException;
import com.example.pg.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 카드사 코드별 빌링키 포트(전략) 레지스트리.
 * ACTIVE 카드사마다 HttpCardCompanyBillingKeyAdapter 인스턴스를 생성해 등록한다.
 * 카드사 추가 시 재기동 후 반영되며, 필요 시 refresh API로 갱신 가능.
 */
@Slf4j
@Component
public class CardCompanyPortRegistry {

    private final CardCompanyRepository cardCompanyRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final Map<String, CardCompanyBillingKeyPort> portByCode = new ConcurrentHashMap<>();

    public CardCompanyPortRegistry(CardCompanyRepository cardCompanyRepository, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.cardCompanyRepository = cardCompanyRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        refresh();
    }

    public void refresh() {
        portByCode.clear();
        List<CardCompany> companies = cardCompanyRepository.findByStatusOrderByDisplayOrderAsc(CardCompanyStatus.ACTIVE);
        for (CardCompany company : companies) {
            if (company.getBaseUrl() != null && !company.getBaseUrl().isBlank()) {
                CardCompanyBillingKeyPort adapter = new HttpCardCompanyBillingKeyAdapter(company.getBaseUrl(), restTemplate, objectMapper);
                portByCode.put(company.getCode(), adapter);
                log.debug("Card company port registered: code={}, baseUrl={}", company.getCode(), company.getBaseUrl());
            }
        }
    }

    public Optional<CardCompanyBillingKeyPort> getPort(String cardCompanyCode) {
        return Optional.ofNullable(portByCode.get(cardCompanyCode));
    }

    public CardCompanyBillingKeyPort getPortOrThrow(String cardCompanyCode) {
        return getPort(cardCompanyCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.CARD_COMPANY_NOT_FOUND, cardCompanyCode));
    }
}
