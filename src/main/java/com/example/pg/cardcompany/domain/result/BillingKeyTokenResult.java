package com.example.pg.cardcompany.domain.result;

/**
 * 카드사 빌링키 발급 응답 (가이드 3.3).
 * PG는 billingKeyToken을 가맹점/사용자와 매핑해 저장하고 결제 승인 등에 사용한다.
 */
public record BillingKeyTokenResult(
        String billingKeyToken
) {}
