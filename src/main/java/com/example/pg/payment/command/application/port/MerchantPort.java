package com.example.pg.payment.command.application.port;

/**
 * 영수증 발급 시 가맹점명을 조회하는 포트 (안티커럽션).
 * receipt는 merchant 인프라에 직접 의존하지 않고 이 포트만 사용한다.
 */
public interface MerchantPort {

    /**
     * 가맹점 ID로 가맹점명 조회.
     * 없으면 빈 문자열 반환.
     */
    String getMerchantName(String merchantId);
}
