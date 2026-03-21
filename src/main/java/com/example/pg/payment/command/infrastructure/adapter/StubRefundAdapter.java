package com.example.pg.payment.command.infrastructure.adapter;

import com.example.pg.payment.command.application.port.RefundPort;
import org.springframework.stereotype.Component;

/**
 * 카드사/은행 연동이 없을 때 사용하는 스텁.
 * 환불 요청을 항상 성공으로 처리한다.
 * 실제 연동 시 RefundPort 구현체를 카드사 API 호출로 교체한다.
 */
@Component
public class StubRefundAdapter implements RefundPort {

    @Override
    public boolean requestRefund(String paymentId, long amount) {
        // 스텁: 환불 항상 성공
        return true;
    }
}
