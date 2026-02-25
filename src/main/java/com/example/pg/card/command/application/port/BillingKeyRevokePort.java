package com.example.pg.card.command.application.port;

/**
 * 카드사/은행에 빌링키 차단(해지)을 요청하는 아웃바운드 포트.
 * 가맹점 삭제 시 해당 가맹점에 발급된 빌링키를 카드사에 차단 요청할 때 사용한다.
 */
public interface BillingKeyRevokePort {

    /**
     * 카드사/은행에 빌링키 차단(해지) 요청.
     *
     * @param billingKey 차단할 빌링키
     */
    void revokeBillingKey(String billingKey);
}
