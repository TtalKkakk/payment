package com.example.pg.receipt.application;

import com.example.pg.common.exception.BusinessException;
import com.example.pg.common.exception.ErrorCode;
import com.example.pg.receipt.domain.aggregate.Receipt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptOrchestratorService {
    private final ReceiptService receiptService;

    public byte[] generateByPaymentId(String paymentId, String merchantId) {
        Receipt receipt = receiptService.getByPaymentId(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECEIPT_NOT_FOUND, paymentId));
        if (!receipt.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ErrorCode.RECEIPT_NOT_FOUND, paymentId);
        }
        return receiptService.generate(receipt);
    }
}
