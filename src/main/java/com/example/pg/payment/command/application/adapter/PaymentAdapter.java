package com.example.pg.payment.command.application.adapter;

import com.example.pg.payment.presentation.port.PaymentPort;
import com.example.pg.payment.presentation.port.dto.PaymentSnapshotForReceiptDto;
import com.example.pg.payment.domain.enumerate.PaymentStatus;
import com.example.pg.payment.infrastructure.persistence.PaymentRepository;
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

    private final PaymentRepository paymentRepository;

    @Override
    public boolean existsPayment(String paymentId) {
        return paymentRepository.existsById(paymentId);
    }

    @Override
    public Optional<PaymentSnapshotForReceiptDto> findAuthorizedPayment(String paymentId) {
        return paymentRepository.findById(paymentId)
                .filter(p -> p.getStatus() == PaymentStatus.AUTHORIZED)
                .map(p -> new PaymentSnapshotForReceiptDto(
                        p.getId(),
                        p.getMerchantId(),
                        p.getAmount(),
                        p.getOrderName(),
                        p.getMerchantOrderId(),
                        p.getCustomerName(),
                        p.getApprovalNumber(),
                        p.getTransactionId(),
                        p.getApprovedAt()
                ));
    }
}
