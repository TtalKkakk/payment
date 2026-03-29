package com.example.pg.payment.application;

import com.example.pg.card_company.domain.aggergate.CardCompany;
import com.example.pg.card_company.presentation.port.CardCompanyPort;
import com.example.pg.merchant.presentation.port.MerchantPort;
import com.example.pg.payment.application.adapter.dto.PaymentSnapshotForReceiptDto;
import com.example.pg.payment.application.dto.PaymentWebhookDto;
import com.example.pg.common.presentation.FranchiseConnect;
import com.example.pg.payment.domain.aggregate.Payment;
import com.example.pg.payment.domain.enumerate.PaymentStatus;
import com.example.pg.payment.domain.event.AuthorizationStartedEvent;
import com.example.pg.payment.domain.event.CancellationStartedEvent;
import com.example.pg.payment.domain.event.PaymentCreatedEvent;
import com.example.pg.payment.domain.vo.PaymentId;
import com.example.pg.payment.domain.vo.PaymentMerchantId;
import com.example.pg.common.exception.BusinessException;
import com.example.pg.common.exception.ErrorCode;
import com.example.pg.payment.infrastructure.persistence.PaymentRepository;
import com.example.pg.payment.presentation.dto.PaymentDetailResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {
    private static final String HMAC_SHA256 = "HmacSHA256";

    private final PaymentRepository paymentRepository;
    private final CardCompanyPort cardCompanyPort;
    private final ApplicationEventPublisher eventPublisher;
    private final MerchantPort merchantPort;
    private final ObjectMapper objectMapper;
    private final FranchiseConnect franchiseConnect;

    /**
     * 트랜잭션 1: 결제 객체 생성 → 상태 READY.
     * 트랜잭션 실패 시 가맹점에게 결제 생성 실패 및 사유 전달.
     */

    @Transactional
    public PaymentId createPayment(String merchantId,
                                   long amount,
                                   String merchantOrderId,
                                   String orderName,
                                   String customerEmail,
                                   String customerName,
                                   String callbackUrl,
                                   String cardCompanyCode) {
        log.debug("[Payment] createPayment start merchantId={} amount={} cardCompanyCode={}", merchantId, amount, cardCompanyCode);
        PaymentId paymentId = PaymentId.generate();
        CardCompany cardCompany = cardCompanyPort.getCardCompanyByCode(cardCompanyCode);
        Payment payment = new Payment(
                paymentId,
                merchantId,
                amount,
                merchantOrderId,
                orderName,
                customerEmail,
                customerName,
                callbackUrl,
                cardCompany
        );
        paymentRepository.save(payment);

        eventPublisher.publishEvent(PaymentCreatedEvent.from(payment.getId(), payment.getMerchantId(), payment.getAmount()));

        log.debug("[Payment] createPayment committed paymentId={} merchantId={}", paymentId.getValue(), merchantId);
        return paymentId;
    }

    /**
     * 빌링키로 결제 승인을 시작한다. 상태를 AUTHORIZING으로 변경하고,
     * 비동기 프로세스가 카드사에 빌링키 청구 요청을 수행하도록 위임한다.
     */
    @Transactional
    public void startAuthorization(String paymentIdValue, String billingKey) {
        log.debug("[Payment] startAuthorization start paymentId={}", paymentIdValue);

        Payment payment = paymentRepository.load(PaymentId.from(paymentIdValue))
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND, paymentIdValue));

        payment.startAuthorization();

        // 트랜잭션 커밋 후 비동기 승인 처리 (커밋 전 호출 시 DB에 AUTHORIZING이 반영되기 전에 조회되어 상태가 바뀌지 않는 문제 방지)
        eventPublisher.publishEvent(AuthorizationStartedEvent.from(paymentIdValue, billingKey));
        log.debug("[Payment] startAuthorization committed paymentId={}", paymentIdValue);
    }

    /** Tx2(승인 시작) 실패 시 보상: READY 결제를 ABORTED로 무효화. 별도 트랜잭션으로 실행 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void compensateCreationFailure(String paymentId) {
        paymentRepository.findByIdAndStatus(paymentId, PaymentStatus.READY)
                .ifPresent(Payment::markAsAborted);
        // 재시도 처리
    }

    /**
     * 결제 취소(환불)를 시작한다. AUTHORIZED → CANCELLING 후 커밋되면 비동기로 카드사 환불 요청을 수행한다.
     * 성공 시 CANCELED 및 웹훅, 실패 시 CANCEL_FAILED 및 웹훅. CANCEL_FAILED인 결제는 취소 API로 재시도 가능.
     */
    @Transactional
    public void startCancellation(String merchantId, String paymentIdValue) {
        log.debug("[Payment] startCancellation merchantId={} paymentId={}", merchantId, paymentIdValue);
        Payment payment = paymentRepository.load(PaymentId.from(paymentIdValue))
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND, paymentIdValue));

        if (!merchantId.equals(payment.getMerchantId())) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_FOUND, paymentIdValue);
        }

        payment.startCancellation();

        eventPublisher.publishEvent(CancellationStartedEvent.from(paymentIdValue));
        log.debug("[Payment] startCancellation committed paymentId={}", paymentIdValue);
    }

    @Transactional(readOnly = true)
    public Optional<PaymentDetailResponse> getPayment(String merchantId, String paymentId) {
        PaymentId validatedPaymentId = PaymentId.from(paymentId);
        String paymentIdValue = validatedPaymentId.getValue();

        log.debug("[Payment] Query getPayment merchantId={} paymentId={}", merchantId, paymentIdValue);
        return paymentRepository.findByMerchantIdAndId(new PaymentMerchantId(merchantId), paymentIdValue)
                .map(PaymentDetailResponse::from);
    }

    @Transactional(readOnly = true)
    public boolean existsPayment(String paymentId) {
        return paymentRepository.existsById(paymentId);
    }

    @Transactional(readOnly = true)
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
    /**
     * 결제 상태 변경 결과를 가맹점 callbackUrl로 웹훅 발송.
     * callbackUrl이 없으면 발송하지 않는다. 서명은 가맹점 apiSecret으로 생성한다.
     */
    public void sendWebhook(Payment payment) {
        String callbackUrl = payment.getCallbackUrl();

        String apiSecret = merchantPort.getApiSecret(payment.getMerchantId());

        PaymentWebhookDto payload = PaymentWebhookDto.from(payment);
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("[Payment] webhook payload serialize failed paymentId={}", payment.getId(), e);
            return;
        }

        String signature = null;
        try {
            signature = computeHmacSha256(payloadJson, apiSecret);
        } catch (Exception e) {
            log.warn("[Payment] webhook signature failed paymentId={}", payment.getId(), e);
        }

        franchiseConnect.send(callbackUrl, payloadJson, signature);
        log.info("[Payment] webhook sent paymentId={} status={}", payment.getId(), payment.getStatus());
    }

    private static String computeHmacSha256(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance(HMAC_SHA256);
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }
}
