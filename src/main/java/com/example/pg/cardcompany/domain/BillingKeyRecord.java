package com.example.pg.cardcompany.domain;

import lombok.Getter;

/**
 * 카드사가 발급한 빌링키 정보.
 * (분리 시 카드사 전용 DB/엔티티로 이전)
 */
@Getter
public class BillingKeyRecord {

    private final String cardToken;
    private final String billingKeyValue;
    private final String merchantId;

    public BillingKeyRecord(String cardToken, String billingKeyValue, String merchantId) {
        this.cardToken = cardToken;
        this.billingKeyValue = billingKeyValue;
        this.merchantId = merchantId;
    }
}
