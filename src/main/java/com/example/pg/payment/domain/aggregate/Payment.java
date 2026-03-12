package com.example.pg.payment.domain.aggregate;

import com.example.pg.payment.domain.vo.PaymentId;
import com.example.pg.payment.domain.enumerate.PaymentStatus;
import com.example.pg.exception.BusinessException;
import com.example.pg.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 결제 애그리거트 (카드 결제 전용)
 * 영수증 발급·거래 추적을 위한 가맹점 주문정보·고객정보를 보관한다.
 */
@Entity
@Table(name = "payments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Payment {
    @Id
    private String id;

    @Column(nullable = false, length = 36, name = "merchant_id")
    private String merchantId;

    private long amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Column(length = 100, name = "merchant_order_id")
    private String merchantOrderId;

    @Column(length = 200, name = "order_name")
    private String orderName;

    @Column(length = 200, name = "customer_email")
    private String customerEmail;

    @Column(length = 100, name = "customer_name")
    private String customerName;

    @Column(length = 500, name = "callback_url")
    private String callbackUrl;

    @Column(length = 50, name = "approval_number")
    private String approvalNumber;

    @Column(length = 100, name = "transaction_id")
    private String transactionId;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_company_id", nullable = true)
    private CardCompany cardCompany;

    public Payment(PaymentId paymentId, String merchantId, long amount,
                   String merchantOrderId, String orderName, String customerEmail, String customerName,
                   String callbackUrl) {
        this(paymentId, merchantId, amount, merchantOrderId, orderName, customerEmail, customerName, callbackUrl, null);
    }

    public Payment(PaymentId paymentId, String merchantId, long amount,
                   String merchantOrderId, String orderName, String customerEmail, String customerName,
                   String callbackUrl, CardCompany cardCompany) {
        this.id = paymentId.getValue();
        this.merchantId = merchantId;
        this.amount = amount;
        this.merchantOrderId = merchantOrderId;
        this.orderName = orderName;
        this.customerEmail = customerEmail;
        this.customerName = customerName;
        this.callbackUrl = callbackUrl;
        this.cardCompany = cardCompany;
        this.status = PaymentStatus.READY;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
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
        this.status = PaymentStatus.FAILED;
        this.updatedAt = LocalDateTime.now();
    }

    public void cancel() {
        if (status != PaymentStatus.AUTHORIZED) {
            throw new BusinessException(ErrorCode.PAYMENT_INVALID_STATUS, "결제 취소 불가: " + status);
        }
        this.status = PaymentStatus.CANCELED;
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
}
