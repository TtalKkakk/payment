package com.example.pg.payment.infrastructure.adapter;

import com.example.pg.payment.domain.enumerate.PaymentStatus;
import com.example.pg.payment.infrastructure.persistence.PaymentRepository;
import com.example.pg.receipt.command.application.port.PaymentPort;
import com.example.pg.receipt.command.application.port.dto.PaymentSnapshotForReceipt;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * payment 쪽 어댑터. 영수증용 결제 스냅샷만 제공 (payment 도메인만 의존).
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
    public Optional<PaymentSnapshotForReceipt> findAuthorizedPayment(String paymentId) {
        return paymentRepository.findById(paymentId)
                .filter(p -> p.getStatus() == PaymentStatus.AUTHORIZED)
                .map(p -> new PaymentSnapshotForReceipt(
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
