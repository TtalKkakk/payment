package com.example.pg.merchantapplication.domain.enumerate;

/**
 * 가맹점 신청 상태.
 */
public enum MerchantApplicationStatus {
    /** 심사 대기 */
    PENDING,
    /** 승인됨 (Merchant 생성 완료) */
    APPROVED,
    /** 거절됨 */
    REJECTED,
    /** 신청 취소 (사용자가 심사 전 철회) */
    CANCELLED,
    /** 구독 종료 (승인 후 구독 취소로 Merchant 비활성/삭제됨) */
    SUBSCRIPTION_ENDED
}
