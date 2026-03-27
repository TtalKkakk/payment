package com.example.pg.payment.presentation;

import com.example.pg.common.config.filter.MerchantAuthFilter;
import com.example.pg.common.exception.BusinessException;
import com.example.pg.common.exception.ErrorCode;
import com.example.pg.payment.application.PaymentCreationOrchestratorService;
import com.example.pg.payment.application.PaymentService;
import com.example.pg.payment.domain.vo.PaymentId;
import com.example.pg.payment.presentation.dto.AuthorizePaymentRequest;
import com.example.pg.payment.presentation.dto.PaymentDetailResponse;
import com.example.pg.payment.presentation.dto.CreatePaymentRequest;
import com.example.pg.payment.presentation.dto.CreatePaymentResponse;
import com.example.pg.receipt.command.application.ReceiptPdfService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentCreationOrchestratorService paymentCreationOrchestratorService;
    private final ReceiptPdfService receiptPdfService;

    /**
     * 결제 단건 조회.
     * 폴링·상태 확인 시 사용. 해당 가맹점의 결제만 조회할 수 있다.
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentDetailResponse> getPayment(
            @RequestAttribute(MerchantAuthFilter.MERCHANT_ID_ATTRIBUTE) String merchantId,
            @PathVariable String paymentId
    ) {
        log.debug("[Payment] API getPayment merchantId={} paymentId={}", merchantId, paymentId);
        return paymentService.getPayment(merchantId, paymentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 결제하기 (단일 API).
     * - 트랜잭션 1: 결제 객체 생성 → READY. 실패 시 4xx/5xx.
     * - 트랜잭션 2: 결제 승인 요청 → AUTHORIZING. 실패 시 4xx/5xx.
     * - 이후 비동기로 카드사 응답 후 AUTHORIZED/FAILED 및 웹훅.
     * 응답: HTTP CREATED + paymentId (두 트랜잭션 모두 성공 시).
     */
    @PostMapping
    public ResponseEntity<CreatePaymentResponse> createPayment(
            @RequestAttribute(MerchantAuthFilter.MERCHANT_ID_ATTRIBUTE) String merchantId,
            @RequestBody CreatePaymentRequest request
    ) {
        log.debug("[Payment] API createPayment merchantId={} amount={}", merchantId, request.amount());
        PaymentId paymentId = paymentCreationOrchestratorService.createPaymentAndStartAuthorization(
                merchantId,
                request
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CreatePaymentResponse(paymentId.getValue()));
    }

    /**
     * 같은 결제로 승인만 재시도 (Tx2만 실행). READY 상태일 때만 호출 가능.
     * E030(승인 요청 실패) 응답 후 가맹점 프론트에서 "승인만 재시도" 시 이 API 사용.
     */
    @PostMapping("/{paymentId}/authorize")
    public ResponseEntity<Void> startAuthorization(
            @RequestAttribute(MerchantAuthFilter.MERCHANT_ID_ATTRIBUTE) String merchantId,
            @PathVariable String paymentId,
            @RequestBody AuthorizePaymentRequest request
    ) {
        log.debug("[Payment] API startAuthorization merchantId={} paymentId={}", merchantId, paymentId);
        paymentService.startAuthorization(paymentId, request.billingKey());
        return ResponseEntity.accepted().build();
    }

    /**
     * 결제 취소(환불)를 요청한다.
     * AUTHORIZED 상태의 결제에 대해서만 취소 가능하다.
     * 해당 가맹점의 결제만 취소할 수 있다.
     */
    @PostMapping("/{paymentId}/cancel")
    public ResponseEntity<Void> cancelPayment(
            @RequestAttribute(MerchantAuthFilter.MERCHANT_ID_ATTRIBUTE) String merchantId,
            @PathVariable String paymentId
    ) {
        log.debug("[Payment] API cancelPayment merchantId={} paymentId={}", merchantId, paymentId);
        paymentService.cancelPayment(merchantId, paymentId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 해당 결제의 영수증 PDF 다운로드.
     * 해당 가맹점의 결제에 대해서만 발급 가능.
     */
    @GetMapping("/{paymentId}/receipt/pdf")
    public ResponseEntity<byte[]> getReceiptPdf(
            @RequestAttribute(MerchantAuthFilter.MERCHANT_ID_ATTRIBUTE) String merchantId,
            @PathVariable String paymentId
    ) {
        log.debug("[Payment] API getReceiptPdf merchantId={} paymentId={}", merchantId, paymentId);
        if (paymentId == null || paymentId.isBlank() || ";".equals(paymentId)) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_FOUND, paymentId);
        }
        byte[] pdf = receiptPdfService.generateByPaymentId(paymentId, merchantId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "receipt-" + paymentId + ".pdf");
        headers.setContentLength(pdf.length);
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }
}
