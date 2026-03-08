package com.example.pg.cardcompany.domain.enumerate;

/**
 * PG에서 이용 가능한 카드사 상태.
 */
public enum CardCompanyStatus {
    /** 연동 가능 */
    ACTIVE,
    /** 연동 중단(점검 등) */
    INACTIVE
}
