package com.example.pg.card.infrastructure.adapter;

import com.example.pg.card.command.application.port.BillingKeyIssuePort;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 카드사/은행 연동이 없을 때 사용하는 스텁.
 * cardToken을 받아 PG에서 생성한 값을 빌링키 형태로 반환한다.
 * 실제 연동 시 BillingKeyIssuePort 구현체를 카드사 API 호출로 교체하면 된다.
 */
@Component
public class StubBillingKeyIssueAdapter implements BillingKeyIssuePort {

    @Override
    public String issueBillingKey(String cardToken) {
        return "bk_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
    }
}
