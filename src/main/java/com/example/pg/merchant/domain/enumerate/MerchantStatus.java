package com.example.pg.merchant.domain.enumerate;

/**
 * 가맹점 상태.
 */
public enum MerchantStatus {
    /** 활성 - API 사용 가능 */
    ACTIVE,
    /** 정지 - API 사용 불가 */
    SUSPENDED,
    /** 탈퇴/폐점 */
    WITHDRAWN
}
