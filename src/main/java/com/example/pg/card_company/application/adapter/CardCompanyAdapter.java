package com.example.pg.card_company.application.adapter;

import com.example.pg.card_company.domain.aggergate.CardCompany;
import com.example.pg.card_company.domain.vo.CardCompanyCode;
import com.example.pg.card_company.infrastructure.persistence.CardCompanyRepository;
import com.example.pg.card_company.presentation.port.CardCompanyPort;
import com.example.pg.common.exception.BusinessException;
import com.example.pg.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CardCompanyAdapter implements CardCompanyPort {
    private final CardCompanyRepository cardCompanyRepository;
    @Override
    public CardCompany getCardCompanyByCode(String code) {
        log.debug("[CardCompany] Query findByCode cardCompanyCode={}", code);
        return cardCompanyRepository.findByCode(new CardCompanyCode(code))
                .orElseThrow(() -> new BusinessException(ErrorCode.CARD_COMPANY_NOT_FOUND, code));
    }
}
