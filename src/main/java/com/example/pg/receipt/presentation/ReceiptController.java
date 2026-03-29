package com.example.pg.receipt.presentation;

import com.example.pg.common.config.filter.MerchantAuthFilter;
import com.example.pg.payment.domain.vo.PaymentId;
import com.example.pg.receipt.application.ReceiptOrchestratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/receipt")
@RequiredArgsConstructor
public class ReceiptController {
    private final ReceiptOrchestratorService receiptOrchestratorService;

    /**
     * 해당 결제의 영수증 PDF 다운로드.
     * 해당 가맹점의 결제에 대해서만 발급 가능.
     */
    @GetMapping("/{paymentId}/pdf")
    public ResponseEntity<byte[]> getReceiptPdf(
            @RequestAttribute(MerchantAuthFilter.MERCHANT_ID_ATTRIBUTE) String merchantId,
            @PathVariable String paymentId
    ) {
        String paymentIdValue = PaymentId.from(paymentId).getValue();

        log.debug("[Payment] API getReceiptPdf merchantId={} paymentId={}", merchantId, paymentIdValue);
        byte[] pdf = receiptOrchestratorService.generateByPaymentId(paymentIdValue, merchantId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "receipt-" + paymentIdValue + ".pdf");
        headers.setContentLength(pdf.length);
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }
}
