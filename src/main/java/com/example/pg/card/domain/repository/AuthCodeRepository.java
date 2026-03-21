package com.example.pg.card.domain.repository;

import java.util.Optional;

import com.example.pg.card.domain.aggregate.AuthCode;

public interface AuthCodeRepository {

    void save(AuthCode authCode);

    Optional<AuthCode> findByCode(String code);

    /** 가맹점 삭제 시 해당 가맹점의 미사용 AuthCode 전부 삭제 */
    void deleteByMerchantId(String merchantId);
}
