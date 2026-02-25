package com.example.pg.card.command.application.port;

/**
 * 카드사/은행에 빌링키 발급을 요청하는 아웃바운드 포트.
 * authCode 검증 후, 해당 카드(cardToken)에 대한 빌링키를 발급받을 때 사용한다.
 */
public interface BillingKeyIssuePort {

    /**
     * 카드사/은행에 빌링키 발급 요청.
     *
     * @param cardToken PG 내부 카드 참조 (Card.token)
     * @return 카드사/은행이 발급한 빌링키
     */
    String issueBillingKey(String cardToken);
}
