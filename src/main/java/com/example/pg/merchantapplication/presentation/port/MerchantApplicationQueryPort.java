package com.example.pg.merchantapplication.presentation.port;

import com.example.pg.merchantapplication.domain.aggregate.MerchantApplication;

/**
 * MerchantApplication 조회 전용 포트.
 * 다른 도메인(예: Merchant)에서 필요한 읽기 의존만 분리해 순환 참조를 방지한다.
 */
public interface MerchantApplicationQueryPort {
    MerchantApplication getByBusinessNumber(String businessNumber);
}
