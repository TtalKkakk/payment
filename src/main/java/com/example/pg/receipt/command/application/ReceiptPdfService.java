package com.example.pg.receipt.command.application;

import com.example.pg.common.exception.BusinessException;
import com.example.pg.common.exception.ErrorCode;
import com.example.pg.receipt.command.application.port.ReceiptPdfPort;
import com.example.pg.receipt.domain.aggregate.Receipt;
import com.example.pg.receipt.query.application.ReceiptQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 영수증 PDF 발급 서비스.
 * 결제 ID로 영수증을 조회한 뒤 PDF 바이트를 생성한다.
 */
@Service
@RequiredArgsConstructor
public class ReceiptPdfService {

    private final ReceiptQueryService receiptQueryService;
    private final ReceiptPdfPort receiptPdfPort;

    /**
     * 결제 ID에 해당하는 영수증의 PDF 생성.
     * 해당 가맹점의 영수증만 발급 가능하다.
     *
     * @param paymentId  결제 ID
     * @param merchantId 가맹점 ID (인증된 가맹점)
     * @return PDF 바이트 (application/pdf)
     * @throws BusinessException RECEIPT_NOT_FOUND 영수증이 없거나 다른 가맹점 소유일 때
     */
    public byte[] generateByPaymentId(String paymentId, String merchantId) {
        Receipt receipt = receiptQueryService.getByPaymentId(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECEIPT_NOT_FOUND, paymentId));
        if (!receipt.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ErrorCode.RECEIPT_NOT_FOUND, paymentId);
        }
        return receiptPdfPort.generate(receipt);
    }
}
