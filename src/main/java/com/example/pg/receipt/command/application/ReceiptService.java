package com.example.pg.receipt.command.application;

import com.example.pg.common.exception.BusinessException;
import com.example.pg.common.exception.ErrorCode;
import com.example.pg.receipt.command.application.port.MerchantPort;
import com.example.pg.receipt.command.application.port.PaymentPort;
import com.example.pg.receipt.command.application.port.dto.PaymentSnapshotForReceipt;
import com.example.pg.receipt.domain.aggregate.Receipt;
import com.example.pg.receipt.domain.vo.ReceiptId;
import com.example.pg.receipt.infrastructure.persistence.ReceiptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 영수증 발급 서비스.
 * 결제 승인 완료(AUTHORIZED)인 결제에 대해 영수증을 생성한다.
 * 이미 해당 결제로 영수증이 있으면 기존 영수증을 반환한다(멱등).
 */
@Service
@RequiredArgsConstructor
public class ReceiptService {

    private final PaymentPort paymentPort;
    private final MerchantPort merchantPort;
    private final ReceiptRepository receiptRepository;

    /**
     * 결제 ID로 영수증 발급.
     * 결제가 승인 완료(AUTHORIZED)일 때만 발급하며, 이미 발급된 경우 기존 영수증을 반환한다.
     *
     * @param paymentId 결제 ID (문자열)
     * @return 발급된(또는 기존) 영수증
     */
    @Transactional
    public Receipt issueForPayment(String paymentId) {
        if (!paymentPort.existsPayment(paymentId)) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_FOUND, paymentId);
        }

        PaymentSnapshotForReceipt snapshot = paymentPort.findAuthorizedPayment(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECEIPT_CANNOT_ISSUE));

        // 이미 해당 결제로 영수증이 있으면 기존 반환 (멱등)
        var existing = receiptRepository.findByPaymentId(snapshot.paymentId());
        if (existing.isPresent()) {
            return existing.get();
        }

        String merchantName = merchantPort.getMerchantName(snapshot.merchantId());

        Receipt receipt = new Receipt(
                ReceiptId.generate(),
                snapshot.paymentId(),
                snapshot.merchantId(),
                merchantName,
                snapshot.amount(),
                snapshot.orderName(),
                snapshot.merchantOrderId(),
                snapshot.customerName(),
                snapshot.approvalNumber(),
                snapshot.transactionId(),
                snapshot.approvedAt()
        );

        return receiptRepository.save(receipt);
    }
}
