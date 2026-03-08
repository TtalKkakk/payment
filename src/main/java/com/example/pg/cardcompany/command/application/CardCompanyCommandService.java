package com.example.pg.cardcompany.command.application;

import com.example.pg.cardcompany.domain.aggregate.CardCompany;
import com.example.pg.cardcompany.infrastructure.persistence.CardCompanyRepository;
import com.example.pg.exception.BusinessException;
import com.example.pg.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 카드사 등록·수정 명령. (관리자 또는 초기 데이터 설정용)
 */
@Service
@RequiredArgsConstructor
public class CardCompanyCommandService {

    private final CardCompanyRepository cardCompanyRepository;

    /**
     * PG에서 이용할 카드사를 등록한다. code 중복 시 예외.
     */
    @Transactional
    public CardCompany register(String code, String name, String baseUrl) {
        if (cardCompanyRepository.findByCode(code).isPresent()) {
            throw new BusinessException(ErrorCode.CARD_COMPANY_DUPLICATE_CODE, code);
        }
        CardCompany entity = CardCompany.create(code, name, baseUrl);
        return cardCompanyRepository.save(entity);
    }
}
