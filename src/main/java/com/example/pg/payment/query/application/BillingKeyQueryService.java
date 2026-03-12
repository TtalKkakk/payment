package com.example.pg.payment.query.application;

import com.example.pg.payment.domain.aggregate.CardCompany;
import com.example.pg.payment.domain.enumerate.CardCompanyStatus;
import com.example.pg.payment.infrastructure.persistence.CardCompanyRepository;
import com.example.pg.exception.BusinessException;
import com.example.pg.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BillingKeyQueryService {

    private final CardCompanyRepository cardCompanyRepository;

    public CardCompany findByCode(String code) {
        return cardCompanyRepository.findByCode(code)
                .orElseThrow(() -> new BusinessException(ErrorCode.CARD_COMPANY_NOT_FOUND, code));
    }

    public Optional<CardCompany> findByCodeOptional(String code) {
        return cardCompanyRepository.findByCode(code);
    }

    public List<CardCompany> findAllActive() {
        return cardCompanyRepository.findByStatusOrderByDisplayOrderAsc(CardCompanyStatus.ACTIVE);
    }
}
