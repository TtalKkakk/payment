package com.example.pg.card.infrastructure.adapter;

import com.example.pg.card.command.application.port.BillingKeyRevokePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 카드사/은행 연동이 없을 때 사용하는 스텁.
 * 빌링키 차단 요청을 로그로만 남기고 실제 API 호출은 하지 않는다.
 * 실제 연동 시 BillingKeyRevokePort 구현체를 카드사 API 호출로 교체하면 된다.
 */
@Slf4j
@Component
public class StubBillingKeyRevokeAdapter implements BillingKeyRevokePort {

    @Override
    public void revokeBillingKey(String billingKey) {
        log.info("[Stub] BillingKeyRevoke requested. billingKey={} (no-op, card company not connected)", billingKey);
    }
}
