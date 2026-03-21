package com.example.pg.payment.presentation;

import com.example.pg.config.filter.MerchantAuthFilter;
import com.example.pg.payment.command.application.PaymentService;
import com.example.pg.payment.query.application.PaymentQueryService;
import com.example.pg.payment.command.domain.vo.PaymentId;
import com.example.pg.payment.presentation.dto.AuthorizePaymentRequest;
import com.example.pg.payment.presentation.dto.PaymentDetailResponse;
import com.example.pg.payment.presentation.dto.CreatePaymentRequest;
import com.example.pg.payment.presentation.dto.CreatePaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentQueryService paymentQueryService;

    /**
     * 결제 단건 조회.
     * 폴링·상태 확인 시 사용. 해당 가맹점의 결제만 조회할 수 있다.
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentDetailResponse> getPayment(
            @RequestAttribute(MerchantAuthFilter.MERCHANT_ID_ATTRIBUTE) String merchantId,
            @PathVariable String paymentId
    ) {
        return paymentQueryService.getPayment(merchantId, paymentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 결제 객체를 생성한다. (상점 서버가 호출)
     * 영수증 발급·거래 추적을 위해 merchantOrderId, orderName, customerEmail, customerName을 함께 전달한다.
     */
    @PostMapping
    public ResponseEntity<CreatePaymentResponse> createPayment(
            @RequestAttribute(MerchantAuthFilter.MERCHANT_ID_ATTRIBUTE) String merchantId,
            @RequestBody CreatePaymentRequest request
    ) {
        PaymentId paymentId = paymentService.createPayment(
                merchantId,
                request.amount(),
                request.merchantOrderId(),
                request.orderName(),
                request.customerEmail(),
                request.customerName(),
                request.callbackUrl()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CreatePaymentResponse(paymentId.getValue()));
    }

    /**
     * 빌링키로 결제 승인을 시작한다.
     * 배달앱 서버가 보유한 빌링키를 전달하여 결제를 진행한다.
     * 비동기적으로 AUTHORIZING → AUTHORIZED/FAILED 로 상태가 변경된다.
     */
    @PostMapping("/{paymentId}/authorize")
    public ResponseEntity<Void> startAuthorization(
            @PathVariable String paymentId,
            @RequestBody AuthorizePaymentRequest request
    ) {
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
        paymentService.cancelPayment(merchantId, paymentId);
        return ResponseEntity.noContent().build();
    }
}
