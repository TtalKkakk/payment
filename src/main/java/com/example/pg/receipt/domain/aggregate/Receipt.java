package com.example.pg.receipt.domain.aggregate;

import com.example.pg.common.exception.BusinessException;
import com.example.pg.common.exception.ErrorCode;
import com.example.pg.receipt.domain.enumerate.ReceiptStatus;
import com.example.pg.receipt.domain.vo.ReceiptId;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 영수증 애그리거트.
 * 결제 승인 완료 시점의 정보 스냅샷을 보관하며, PDF 발급·조회·무효화에 사용한다.
 */
@Entity
@Table(
        name = "receipts",
        uniqueConstraints = @UniqueConstraint(columnNames = "payment_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Receipt {

    @Id
    @Column(length = 36)
    private String id;

    /** 영수증 번호 (발급일-식별자 기반, 인쇄/표시용) */
    @Column(nullable = false, unique = true, length = 50, name = "receipt_number")
    private String receiptNumber;

    @Column(nullable = false, length = 36, name = "payment_id")
    private String paymentId;

    @Column(nullable = false, length = 36, name = "merchant_id")
    private String merchantId;

    /** 가맹점명 스냅샷 */
    @Column(nullable = false, length = 200, name = "merchant_name")
    private String merchantName;

    private long amount;

    @Column(length = 200, name = "order_name")
    private String orderName;

    @Column(length = 100, name = "merchant_order_id")
    private String merchantOrderId;

    @Column(length = 100, name = "customer_name")
    private String customerName;

    @Column(length = 50, name = "approval_number")
    private String approvalNumber;

    @Column(length = 100, name = "transaction_id")
    private String transactionId;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReceiptStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * 결제 승인 완료 시점에 영수증 발급.
     *
     * @param paymentId     결제 ID
     * @param merchantId    가맹점 ID
     * @param merchantName  가맹점명
     * @param amount        결제 금액
     * @param orderName     주문명
     * @param merchantOrderId 가맹점 주문 ID
     * @param customerName  결제자명
     * @param approvalNumber 카드사 승인 번호
     * @param transactionId 카드사 거래 ID
     * @param approvedAt    승인 일시
     */
    public Receipt(
            ReceiptId receiptId,
            String paymentId,
            String merchantId,
            String merchantName,
            long amount,
            String orderName,
            String merchantOrderId,
            String customerName,
            String approvalNumber,
            String transactionId,
            LocalDateTime approvedAt
    ) {
        this.id = receiptId.getValue();
        this.receiptNumber = generateReceiptNumber(receiptId);
        this.paymentId = paymentId;
        this.merchantId = merchantId;
        this.merchantName = merchantName != null ? merchantName : "";
        this.amount = amount;
        this.orderName = orderName;
        this.merchantOrderId = merchantOrderId;
        this.customerName = customerName;
        this.approvalNumber = approvalNumber;
        this.transactionId = transactionId;
        this.approvedAt = approvedAt != null ? approvedAt : LocalDateTime.now();
        this.status = ReceiptStatus.ISSUED;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    private static String generateReceiptNumber(ReceiptId receiptId) {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String suffix = receiptId.getValue().substring(0, Math.min(8, receiptId.getValue().length()));
        return "RCP-" + date + "-" + suffix.toUpperCase();
    }

    /**
     * 결제 취소 등으로 영수증 무효화.
     * 무효화된 영수증은 PDF에 "취소됨" 등으로 표시할 수 있다.
     */
    public void voidReceipt() {
        if (this.status == ReceiptStatus.VOIDED) {
            throw new BusinessException(ErrorCode.RECEIPT_ALREADY_VOIDED);
        }
        this.status = ReceiptStatus.VOIDED;
        this.updatedAt = LocalDateTime.now();
    }

    /** 무효화 여부 */
    public boolean isVoided() {
        return this.status == ReceiptStatus.VOIDED;
    }
}
