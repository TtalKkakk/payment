package com.example.pg.cardcompany.domain;

import java.util.Optional;

/**
 * 카드사가 보유한 빌링키 저장소.
 * 같은 프로젝트일 때는 인메모리/로컬 구현, 분리 시 카드사 전용 DB로 이전.
 */
public interface BillingKeyRepository {

    void save(BillingKeyRecord record);

    Optional<BillingKeyRecord> findByCardToken(String cardToken);

    void deleteByMerchantId(String merchantId);
}
