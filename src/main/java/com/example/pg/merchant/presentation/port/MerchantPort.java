package com.example.pg.merchant.presentation.port;

import com.example.pg.merchant.domain.aggregate.Merchant;
import com.example.pg.merchant.domain.vo.MerchantName;

/**
 * Merchant 도메인과의 연동을 위한 포트.
 * MerchantApplication 등 다른 도메인이 Merchant를 생성·조회할 때 사용하는 계약.
 */
public interface MerchantPort {

    /**
     * 사업자번호·비밀번호가 일치하고 상태가 APPROVED인 신청의 applicationId를 반환.
     * 비밀번호 불일치 또는 미승인 시 예외.
     */
    String findApprovedApplicationIdByBusinessNumberAndPassword(String businessNumber, String rawPassword);

    /**
     * 승인된 신청으로부터 가맹점을 생성 또는 재활성화한다.
     * name은 반드시 MerchantApplication의 이름(MerchantName)을 전달하여 설계상 Merchant.name = MerchantApplication.name 을 보장한다.
     * @return 생성 또는 재활성화된 Merchant
     */
    Merchant createFromApprovedApplication(String applicationId, MerchantName name);

    /**
     * 가맹점 ID로 가맹점명 조회.
     * 없으면 빈 문자열 반환.
     */
    String getMerchantName(String merchantId);
}
