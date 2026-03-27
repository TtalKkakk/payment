package com.example.pg.receipt.command.application;

import com.example.pg.common.exception.BusinessException;
import com.example.pg.common.exception.ErrorCode;
import com.example.pg.merchant.presentation.port.MerchantPort;
import com.example.pg.payment.presentation.port.PaymentPort;
import com.example.pg.payment.application.adapter.dto.PaymentSnapshotForReceiptDto;
import com.example.pg.receipt.domain.aggregate.Receipt;
import com.example.pg.receipt.domain.vo.ReceiptId;
import com.example.pg.receipt.infrastructure.persistence.ReceiptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 영수증 발급 서비스.
 * 결제 승인 완료(AUTHORIZED)인 결제에 대해 영수증을 생성한다.
 * 이미 해당 결제로 영수증이 있으면 기존 영수증을 반환한다(멱등).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptService {

    private final PaymentPort paymentPort;
    private final MerchantPort merchantPort;
    private final ReceiptRepository receiptRepository;

    /**
     * 결제 ID에 대해 영수증 발급. 승인 완료된 결제만 가능하며, 이미 있으면 기존 영수증 반환(멱등).
     */
    public Receipt issueForPayment(String paymentId) {
        log.debug("[Receipt] issueForPayment start paymentId={}", paymentId);
        if (!paymentPort.existsPayment(paymentId)) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_FOUND, paymentId);
        }
        Optional<PaymentSnapshotForReceiptDto> snapshotOpt = paymentPort.findAuthorizedPayment(paymentId);
        if (snapshotOpt.isEmpty()) {
            throw new BusinessException(ErrorCode.RECEIPT_CANNOT_ISSUE);
        }

        Optional<Receipt> existing = receiptRepository.findByPaymentId(paymentId);
        if (existing.isPresent()) {
            log.debug("[Receipt] issueForPayment idempotent-return paymentId={}", paymentId);
            return existing.get();
        }

        PaymentSnapshotForReceiptDto s = snapshotOpt.get();
        String merchantName = merchantPort.getMerchantName(s.merchantId());
        Receipt receipt = new Receipt(
                ReceiptId.generate(),
                s.paymentId(),
                s.merchantId(),
                merchantName,
                s.amount(),
                s.orderName(),
                s.merchantOrderId(),
                s.customerName(),
                s.approvalNumber(),
                s.transactionId(),
                s.approvedAt()
        );
        Receipt saved = receiptRepository.save(receipt);
        log.info("[Receipt] event=Issued paymentId={} receiptId={}", paymentId, saved.getId());
        return saved;
    }
}
