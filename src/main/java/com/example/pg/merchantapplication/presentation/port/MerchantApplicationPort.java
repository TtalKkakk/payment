package com.example.pg.merchantapplication.presentation.port;

import com.example.pg.merchant.domain.aggregate.Merchant;
import com.example.pg.merchant.domain.vo.MerchantName;

/**
 * MerchantApplication 도메인과의 연동을 위한 포트.
 * 다른 도메인(Merchant 등)이 MerchantApplication 상태에 관여할 때 사용하는 계약.
 */
public interface MerchantApplicationPort {

    /**
     * 승인된 신청으로부터 가맹점(Merchant)을 생성 또는 재활성화한다.
     * name은 반드시 해당 MerchantApplication의 이름(MerchantName)을 전달하여 설계상 Merchant.name = MerchantApplication.name 을 보장한다.
     * @return 생성 또는 재활성화된 Merchant
     */
    Merchant createFromApprovedApplication(String applicationId, MerchantName name);

    /**
     * 신청서를 구독 종료(SUBSCRIPTION_ENDED)로 전이한다.
     * Merchant 탈퇴 시 같은 트랜잭션에서 호출. applicationId가 null/blank면 무시.
     */
    void markSubscriptionEnded(String applicationId);
}
