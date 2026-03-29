package com.example.pg.payment.presentation;

import com.example.pg.common.config.filter.MerchantAuthFilter;
import com.example.pg.payment.application.PaymentOrchestratorService;
import com.example.pg.payment.application.PaymentService;
import com.example.pg.payment.domain.vo.PaymentId;
import com.example.pg.payment.presentation.dto.AuthorizePaymentRequest;
import com.example.pg.payment.presentation.dto.PaymentDetailResponse;
import com.example.pg.payment.presentation.dto.CreatePaymentRequest;
import com.example.pg.payment.presentation.dto.CreatePaymentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
    private final PaymentOrchestratorService paymentOrchestratorService;

    /**
     * 결제 단건 조회.
     * 폴링·상태 확인 시 사용. 해당 가맹점의 결제만 조회할 수 있다.
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentDetailResponse> getPayment(
            @RequestAttribute(MerchantAuthFilter.MERCHANT_ID_ATTRIBUTE) String merchantId,
            @PathVariable String paymentId
    ) {
        String paymentIdValue = PaymentId.from(paymentId).getValue();

        log.debug("[Payment] API getPayment merchantId={} paymentId={}", merchantId, paymentIdValue);
        return paymentService.getPayment(merchantId, paymentIdValue)
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
            @Valid @RequestBody CreatePaymentRequest request
    ) {
        log.debug("[Payment] API createPayment merchantId={} amount={}", merchantId, request.amount());
        PaymentId paymentId = paymentOrchestratorService.createPaymentAndStartAuthorization(
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
            @Valid @RequestBody AuthorizePaymentRequest request
    ) {
        String paymentIdValue = PaymentId.from(paymentId).getValue();

        log.debug("[Payment] API startAuthorization merchantId={} paymentId={}", merchantId, paymentIdValue);
        paymentService.startAuthorization(paymentIdValue, request.billingKey());
        return ResponseEntity.accepted().build();
    }

    /**
     * 결제 취소(환불)를 시작한다.
     * AUTHORIZED 또는 CANCEL_FAILED → CANCELLING 후 비동기 환불. 성공 시 CANCELED·웹훅, 실패 시 CANCEL_FAILED·웹훅.
     * 승인 요청과 동일하게 HTTP 202로 수락만 하며, 최종 결과는 폴링·웹훅으로 확인한다.
     */
    @PostMapping("/{paymentId}/cancel")
    public ResponseEntity<Void> cancelPayment(
            @RequestAttribute(MerchantAuthFilter.MERCHANT_ID_ATTRIBUTE) String merchantId,
            @PathVariable String paymentId
    ) {
        String paymentIdValue = PaymentId.from(paymentId).getValue();

        log.debug("[Payment] API cancelPayment merchantId={} paymentId={}", merchantId, paymentIdValue);
        paymentService.startCancellation(merchantId, paymentIdValue);
        return ResponseEntity.accepted().build();
    }
}
