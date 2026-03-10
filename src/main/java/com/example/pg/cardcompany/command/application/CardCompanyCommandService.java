package com.example.pg.cardcompany.command.application;

import com.example.pg.cardcompany.domain.aggregate.CardCompany;
import com.example.pg.cardcompany.application.CardCompanyPortRegistry;
import com.example.pg.cardcompany.infrastructure.persistence.CardCompanyRepository;
import com.example.pg.exception.BusinessException;
import com.example.pg.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CardCompanyCommandService {

    private final CardCompanyRepository cardCompanyRepository;
    private final CardCompanyPortRegistry portRegistry;

    public CardCompany register(String code, String name, String baseUrl) {
        if (cardCompanyRepository.findByCode(code).isPresent()) {
            throw new BusinessException(ErrorCode.INTERNAL);
        }
        CardCompany company = CardCompany.create(code, name, baseUrl);
        cardCompanyRepository.save(company);
        portRegistry.refresh();
        return company;
    }
}
