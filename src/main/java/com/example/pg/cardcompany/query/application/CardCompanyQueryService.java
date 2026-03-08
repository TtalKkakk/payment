package com.example.pg.cardcompany.query.application;

import com.example.pg.cardcompany.domain.aggregate.CardCompany;
import com.example.pg.cardcompany.domain.enumerate.CardCompanyStatus;
import com.example.pg.cardcompany.infrastructure.persistence.CardCompanyRepository;
import com.example.pg.cardcompany.query.application.dto.CardCompanyListItem;
import com.example.pg.exception.BusinessException;
import com.example.pg.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 카드사 조회 전용. PG에서 이용 가능한 카드사 목록·상세 조회.
 */
@Service
@RequiredArgsConstructor
public class CardCompanyQueryService {

    private final CardCompanyRepository cardCompanyRepository;

    /**
     * 연동 가능(ACTIVE)한 카드사 목록. displayOrder 순.
     */
    @Transactional(readOnly = true)
    public List<CardCompanyListItem> findAllActive() {
        return cardCompanyRepository.findByStatusOrderByDisplayOrderAsc(CardCompanyStatus.ACTIVE)
                .stream()
                .map(CardCompanyListItem::from)
                .collect(Collectors.toList());
    }

    /**
     * ID로 카드사 조회.
     */
    @Transactional(readOnly = true)
    public CardCompany findById(String id) {
        return cardCompanyRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CARD_COMPANY_NOT_FOUND, id));
    }

    /**
     * 코드로 카드사 조회 (선택).
     */
    @Transactional(readOnly = true)
    public Optional<CardCompany> findByCode(String code) {
        return cardCompanyRepository.findByCode(code);
    }
}
