package com.example.pg.payment.query.application;

import com.example.pg.payment.domain.aggregate.CardCompany;
import com.example.pg.payment.domain.enumerate.CardCompanyStatus;
import com.example.pg.payment.infrastructure.persistence.CardCompanyRepository;
import com.example.pg.common.exception.BusinessException;
import com.example.pg.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingKeyQueryService {

    private final CardCompanyRepository cardCompanyRepository;

    public CardCompany findByCode(String code) {
        log.debug("[Payment] Query findByCode cardCompanyCode={}", code);
        return cardCompanyRepository.findByCode(code)
                .orElseThrow(() -> new BusinessException(ErrorCode.CARD_COMPANY_NOT_FOUND, code));
    }

    public Optional<CardCompany> findByCodeOptional(String code) {
        log.debug("[Payment] Query findByCodeOptional cardCompanyCode={}", code);
        return cardCompanyRepository.findByCode(code);
    }

    public List<CardCompany> findAllActive() {
        log.debug("[Payment] Query findAllActive cardCompanies");
        return cardCompanyRepository.findByStatusOrderByDisplayOrderAsc(CardCompanyStatus.ACTIVE);
    }
}
