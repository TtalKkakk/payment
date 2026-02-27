package com.example.pg.card.infrastructure.adapter;

import com.example.pg.card.command.application.port.BillingKeyPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 카드사/은행 연동이 없을 때 사용하는 스텁.
 * 발급은 PG에서 생성한 값을 빌링키 형태로 반환하고, 차단은 로그만 남긴다.
 * 실제 연동 시 BillingKeyPort 구현체를 카드사 API 호출로 교체하면 된다.
 */
@Slf4j
@Component
public class StubBillingKeyAdapter implements BillingKeyPort {

    @Override
    public String issueBillingKey(String cardToken) {
        return "bk_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
    }

    @Override
    public void revokeBillingKeysByMerchantId(String merchantId) {
        log.info("[Stub] BillingKeyRevokeByMerchantId requested. merchantId={} (no-op, card company not connected)", merchantId);
    }
}
