package com.example.pg.card_company.application.adapter;

import com.example.pg.card_company.application.CardCompanyService;
import com.example.pg.card_company.domain.aggergate.CardCompany;
import com.example.pg.card_company.presentation.port.CardCompanyPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CardCompanyAdapter implements CardCompanyPort {
    private final CardCompanyService cardCompanyService;
    @Override
    public CardCompany getCardCompanyByCode(String code) {
        return cardCompanyService.findByCode(code);
    }
}
