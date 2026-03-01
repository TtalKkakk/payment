package com.example.pg.card.infrastructure.adapter;

import com.example.pg.card.command.application.port.BillingKeyPort;
import com.example.pg.cardcompany.application.CardCompanyBillingKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * PG → 카드사 연동 (같은 프로젝트).
 * BillingKeyPort를 구현하며, 카드사 서비스를 직접 호출한다.
 * 분리 시 이 구현체를 제거하고, HTTP 호출 Adapter로 교체하면 된다.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "pg.cardcompany.mode", havingValue = "inprocess", matchIfMissing = true)
public class InProcessCardCompanyAdapter implements BillingKeyPort {

    private final CardCompanyBillingKeyService cardCompanyBillingKeyService;

    @Override
    public String issueBillingKey(String cardToken, String merchantId) {
        return cardCompanyBillingKeyService.issueBillingKey(cardToken, merchantId);
    }

    @Override
    public void revokeBillingKeysByMerchantId(String merchantId) {
        cardCompanyBillingKeyService.revokeByMerchantId(merchantId);
    }
}
