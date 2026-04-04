package com.example.pg.payment.domain.aggregate;

import com.example.pg.card_company.domain.aggergate.CardCompany;
import com.example.pg.payment.domain.vo.PaymentId;
import com.example.pg.payment.domain.vo.PaymentMerchantId;
import com.example.pg.payment.domain.vo.PaymentMerchantOrderId;
import com.example.pg.payment.domain.vo.PaymentOrderName;
import com.example.pg.payment.domain.vo.PaymentCustomerEmail;
import com.example.pg.payment.domain.vo.PaymentCustomerName;
import com.example.pg.payment.domain.vo.PaymentCallbackUrl;
import com.example.pg.payment.domain.vo.PaymentAmount;
import com.example.pg.payment.domain.converter.PaymentMerchantIdConverter;
import com.example.pg.payment.domain.converter.PaymentMerchantOrderIdConverter;
import com.example.pg.payment.domain.converter.PaymentOrderNameConverter;
import com.example.pg.payment.domain.converter.PaymentCustomerEmailConverter;
import com.example.pg.payment.domain.converter.PaymentCustomerNameConverter;
import com.example.pg.payment.domain.converter.PaymentCallbackUrlConverter;
import com.example.pg.payment.domain.converter.PaymentAmountConverter;
import com.example.pg.payment.domain.enumerate.PaymentFailureCategory;
import com.example.pg.payment.domain.enumerate.PaymentStatus;
import com.example.pg.common.exception.BusinessException;
import com.example.pg.common.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 결제 애그리거트 (카드 결제 전용).
 * 가맹점·주문번호·금액·카드사는 생성 시 고정({@code updatable = false}). 이후 변경은 상태 전이·승인·실패 스냅샷 등 도메인 규칙에 따른 갱신뿐이다.
 */
@Entity
@Table(
        name = "payments",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_payments_merchant_id_merchant_order_id",
                columnNames = {"merchant_id", "merchant_order_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {
    @Getter
    @Id
    private String id;

    @Column(nullable = false, length = 36, name = "merchant_id", updatable = false)
    @Convert(converter = PaymentMerchantIdConverter.class)
    private PaymentMerchantId merchantId;

    @Column(nullable = false, updatable = false)
    @Convert(converter = PaymentAmountConverter.class)
    private PaymentAmount amount;

    @Getter
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Column(length = 100, name = "merchant_order_id", nullable = false, updatable = false)
    @Convert(converter = PaymentMerchantOrderIdConverter.class)
    private PaymentMerchantOrderId merchantOrderId;

    @Column(length = 200, name = "order_name", nullable = false)
    @Convert(converter = PaymentOrderNameConverter.class)
    private PaymentOrderName orderName;

    @Column(length = 200, name = "customer_email")
    @Convert(converter = PaymentCustomerEmailConverter.class)
    private PaymentCustomerEmail customerEmail;

    @Column(length = 100, name = "customer_name", nullable = false)
    @Convert(converter = PaymentCustomerNameConverter.class)
    private PaymentCustomerName customerName;

    @Column(length = 500, name = "callback_url", nullable = false)
    @Convert(converter = PaymentCallbackUrlConverter.class)
    private PaymentCallbackUrl callbackUrl;

    @Getter
    @Column(length = 50, name = "approval_number")
    private String approvalNumber;

    @Getter
    @Column(length = 100, name = "transaction_id")
    private String transactionId;

    @Getter
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    /**
     * 마지막 실패 스냅샷({@link PaymentStatus#AUTHORIZE_FAILED}, {@link PaymentStatus#CANCEL_FAILED} 등 설정 시 갱신).
     * 승인/취소 성공 시에는 DB 업데이트로 초기화된다.
     */
    @Getter
    @Enumerated(EnumType.STRING)
    @Column(name = "last_failure_category", length = 20)
    private PaymentFailureCategory lastFailureCategory;

    @Getter
    @Column(name = "last_failure_code", length = 100)
    private String lastFailureCode;

    @Getter
    @Column(name = "last_failure_message", length = 2000)
    private String lastFailureMessage;

    @Getter
    @Column(name = "last_failure_at")
    private LocalDateTime lastFailureAt;

    @Getter
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Getter
    private LocalDateTime updatedAt;

    @Getter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_company_id", nullable = false, updatable = false)
    private CardCompany cardCompany;

    protected Payment(PaymentId paymentId,
                      String merchantId,
                      long amount,
                      String merchantOrderId,
                      String orderName,
                      String customerEmail,
                      String customerName,
                      String callbackUrl,
                      CardCompany cardCompany) {
        this.id = paymentId.getValue();
        this.merchantId = new PaymentMerchantId(merchantId);
        this.amount = new PaymentAmount(amount);
        this.merchantOrderId = new PaymentMerchantOrderId(merchantOrderId);
        this.orderName = new PaymentOrderName(orderName);
        this.customerEmail = customerEmail == null || customerEmail.isBlank() ? null : new PaymentCustomerEmail(customerEmail);
        this.customerName = new PaymentCustomerName(customerName);
        this.callbackUrl = new PaymentCallbackUrl(callbackUrl);
        this.cardCompany = cardCompany;
        this.status = PaymentStatus.READY;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public static Payment create(PaymentId paymentId,
                                 String merchantId,
                                 long amount,
                                 String merchantOrderId,
                                 String orderName,
                                 String customerEmail,
                                 String customerName,
                                 String callbackUrl,
                                 CardCompany cardCompany) {
        return new Payment(
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
    }

    public void startAuthorization() {
        if (status != PaymentStatus.READY) {
            throw new BusinessException(ErrorCode.PAYMENT_INVALID_STATUS, "결제 승인 시작 불가: " + status);
        }
        this.status = PaymentStatus.AUTHORIZING;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 카드사 승인 성공 반영. approvalNumber·transactionId·approvedAt 저장 (취소·조회용).
     */
    public void authorizeSuccess(String approvalNumber, String transactionId, LocalDateTime approvedAt) {
        if (status != PaymentStatus.AUTHORIZING) {
            throw new BusinessException(ErrorCode.PAYMENT_INVALID_STATUS, "결제 승인 성공 처리 불가: " + status);
        }
        this.status = PaymentStatus.AUTHORIZED;
        this.approvalNumber = approvalNumber;
        this.transactionId = transactionId;
        this.approvedAt = approvedAt != null ? approvedAt : LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void authorizeFail() {
        if (status != PaymentStatus.AUTHORIZING) {
            throw new BusinessException(ErrorCode.PAYMENT_INVALID_STATUS, "결제 승인 실패 처리 불가: " + status);
        }
        this.status = PaymentStatus.AUTHORIZE_FAILED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 환불(취소) 요청. AUTHORIZED 또는 CANCEL_FAILED(재시도) → CANCELLING.
     */
    public void startCancellation() {
        if (status != PaymentStatus.AUTHORIZED && status != PaymentStatus.CANCEL_FAILED) {
            throw new BusinessException(ErrorCode.PAYMENT_INVALID_STATUS, "결제 취소 시작 불가: " + status);
        }
        this.status = PaymentStatus.CANCELLING;
        this.updatedAt = LocalDateTime.now();
    }

    /** 카드사 환불 성공 반영. CANCELLING → CANCELED */
    public void completeCancellation() {
        if (status != PaymentStatus.CANCELLING) {
            throw new BusinessException(ErrorCode.PAYMENT_INVALID_STATUS, "결제 취소 완료 처리 불가: " + status);
        }
        this.status = PaymentStatus.CANCELED;
        this.updatedAt = LocalDateTime.now();
    }

    /** 카드사 환불 실패 시 CANCELLING → CANCEL_FAILED */
    public void markCancellationFailed() {
        if (status != PaymentStatus.CANCELLING) {
            throw new BusinessException(ErrorCode.PAYMENT_INVALID_STATUS, "취소 실패 처리 불가: " + status);
        }
        this.status = PaymentStatus.CANCEL_FAILED;
        this.updatedAt = LocalDateTime.now();
    }

    /** Tx2(승인 시작) 실패 시 보상: READY → ABORTED */
    public void markAsAborted() {
        if (status != PaymentStatus.READY) {
            throw new BusinessException(ErrorCode.PAYMENT_INVALID_STATUS, "보상(ABORTED) 처리 불가: " + status);
        }
        this.status = PaymentStatus.ABORTED;
        this.updatedAt = LocalDateTime.now();
    }

    public String getMerchantId() {
        return merchantId == null ? null : merchantId.value();
    }

    public long getAmount() {
        return amount == null ? 0L : amount.value();
    }

    public String getMerchantOrderId() {
        return merchantOrderId == null ? null : merchantOrderId.value();
    }

    public String getOrderName() {
        return orderName == null ? null : orderName.value();
    }

    public String getCustomerEmail() {
        return customerEmail == null ? null : customerEmail.value();
    }

    public String getCustomerName() {
        return customerName == null ? null : customerName.value();
    }

    public String getCallbackUrl() {
        return callbackUrl == null ? null : callbackUrl.value();
    }

}
