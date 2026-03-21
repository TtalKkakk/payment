package com.example.pg.card.domain.repository;

import java.util.Optional;

import com.example.pg.card.domain.aggregate.CardRegistrationToken;

public interface CardRegistrationTokenRepository {

    void save(CardRegistrationToken token);

    Optional<CardRegistrationToken> findById(String token);

    Optional<CardRegistrationToken> findByMerchantIdAndOwnerId(String merchantId, String ownerId);

    void delete(CardRegistrationToken token);

    /** 가맹점 삭제 시 해당 가맹점의 카드 등록 토큰 전부 삭제 */
    void deleteByMerchantId(String merchantId);
}
