package com.example.pg.payment.infrastructure.persistence;

import com.example.pg.payment.domain.aggregate.CardCompany;
import com.example.pg.payment.domain.enumerate.CardCompanyStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CardCompanyRepository extends JpaRepository<CardCompany, String> {

    Optional<CardCompany> findByCode(String code);

    List<CardCompany> findByStatusOrderByDisplayOrderAsc(CardCompanyStatus status);
}
