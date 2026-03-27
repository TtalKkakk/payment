package com.example.pg.card_company.presentation.port;

import com.example.pg.card_company.domain.aggergate.CardCompany;

public interface CardCompanyPort {
    CardCompany getCardCompanyByCode(String code);
}
