package com.example.pg.payment.application.adapter;

import com.example.pg.payment.application.PaymentService;
import com.example.pg.payment.application.adapter.dto.PaymentSnapshotForReceiptDto;
import com.example.pg.payment.presentation.port.PaymentPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 영수증 발급용 결제 조회 어댑터.
 * PaymentPort를 구현해 receipt 모듈에 승인 완료 결제 스냅샷을 제공한다.
 */
@Component
@RequiredArgsConstructor
public class PaymentAdapter implements PaymentPort {

    private final PaymentService paymentService;

    @Override
    public boolean existsPayment(String paymentId) {
        return paymentService.existsPayment(paymentId);
    }

    @Override
    public Optional<PaymentSnapshotForReceiptDto> findAuthorizedPayment(String paymentId) {
        return paymentService.findAuthorizedPayment(paymentId);
    }
}
