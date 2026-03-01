package com.example.pg.card.command.application.port;

/**
 * 카드사/은행 빌링키 연동을 위한 아웃바운드 포트.
 * 발급·차단 모두 동일한 카드사와 통신하므로 하나의 포트로 통일한다.
 */
public interface BillingKeyPort {

    /**
     * 카드사/은행에 빌링키 발급 요청.
     *
     * @param cardToken  PG 내부 카드 참조 (Card.token)
     * @param merchantId 가맹점 ID (카드사가 차단 시 가맹점별 조회용으로 보관)
     * @return 카드사/은행이 발급한 빌링키
     */
    String issueBillingKey(String cardToken, String merchantId);

    /**
     * 해당 가맹점에 발급된 모든 빌링키를 카드사/은행에 차단(해지) 요청한다.
     * 카드사가 merchantId와 빌링키 매핑을 보유하므로 PG는 merchantId만 전달한다.
     *
     * @param merchantId 차단 대상 가맹점 ID
     */
    void revokeBillingKeysByMerchantId(String merchantId);
}
