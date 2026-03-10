package com.example.pg.cardcompany.infrastructure.persistence;

import com.example.pg.cardcompany.domain.aggregate.CardCompany;
import com.example.pg.cardcompany.domain.enumerate.CardCompanyStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CardCompanyRepository extends JpaRepository<CardCompany, String> {

    Optional<CardCompany> findByCode(String code);

    List<CardCompany> findByStatusOrderByDisplayOrderAsc(CardCompanyStatus status);
}
