package com.example.pg.card.infrastructure.adapter;

import com.example.pg.card.command.application.port.BillingKeyPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 카드사/은행 연동이 없을 때 사용하는 스텁.
 * pg.cardcompany.mode=stub 이면 이 빈이 사용된다.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "pg.cardcompany.mode", havingValue = "stub")
public class StubBillingKeyAdapter implements BillingKeyPort {

    @Override
    public String issueBillingKey(String cardToken, String merchantId) {
        return "bk_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
    }

    @Override
    public void revokeBillingKeysByMerchantId(String merchantId) {
        log.info("[Stub] BillingKeyRevokeByMerchantId requested. merchantId={} (no-op, card company not connected)", merchantId);
    }
}
