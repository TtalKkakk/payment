package com.example.pg.receipt.domain.aggregate;

import com.example.pg.common.exception.BusinessException;
import com.example.pg.receipt.domain.enumerate.ReceiptStatus;
import com.example.pg.receipt.domain.vo.ReceiptId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Receipt 도메인")
class ReceiptTest {

    private static final String PAYMENT_ID = "pay-001";
    private static final String MERCHANT_ID = "merchant-1";
    private static final String MERCHANT_NAME = "테스트 가맹점";
    private static final long AMOUNT = 15_000L;
    private static final LocalDateTime APPROVED_AT = LocalDateTime.of(2026, 3, 15, 10, 0);

    private Receipt createReceipt(ReceiptId receiptId) {
        return new Receipt(
                receiptId,
                PAYMENT_ID,
                MERCHANT_ID,
                MERCHANT_NAME,
                AMOUNT,
                "주문명",
                "ord-1",
                "홍길동",
                "approval-123",
                "tx-456",
                APPROVED_AT
        );
    }

    @Nested
    @DisplayName("생성자")
    class Constructor {

        @Test
        @DisplayName("필드가 스냅샷대로 설정되고 상태는 ISSUED")
        void setsFieldsAndStatusIssued() {
            ReceiptId id = ReceiptId.generate();
            Receipt receipt = createReceipt(id);

            assertThat(receipt.getId()).isEqualTo(id.getValue());
            assertThat(receipt.getPaymentId()).isEqualTo(PAYMENT_ID);
            assertThat(receipt.getMerchantId()).isEqualTo(MERCHANT_ID);
            assertThat(receipt.getMerchantName()).isEqualTo(MERCHANT_NAME);
            assertThat(receipt.getAmount()).isEqualTo(AMOUNT);
            assertThat(receipt.getOrderName()).isEqualTo("주문명");
            assertThat(receipt.getMerchantOrderId()).isEqualTo("ord-1");
            assertThat(receipt.getCustomerName()).isEqualTo("홍길동");
            assertThat(receipt.getApprovalNumber()).isEqualTo("approval-123");
            assertThat(receipt.getTransactionId()).isEqualTo("tx-456");
            assertThat(receipt.getApprovedAt()).isEqualTo(APPROVED_AT);
            assertThat(receipt.getStatus()).isEqualTo(ReceiptStatus.ISSUED);
            assertThat(receipt.isVoided()).isFalse();
            assertThat(receipt.getCreatedAt()).isNotNull();
            assertThat(receipt.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("영수증 번호는 RCP-날짜-식별자 형식")
        void receiptNumberFormat() {
            ReceiptId id = ReceiptId.from("a1b2c3d4-5678-90ab-cdef-123456789012");
            Receipt receipt = createReceipt(id);

            assertThat(receipt.getReceiptNumber()).startsWith("RCP-");
            assertThat(receipt.getReceiptNumber()).contains("A1B2C3D4");
        }

        @Test
        @DisplayName("merchantName null이면 빈 문자열")
        void merchantNameNullBecomesEmpty() {
            Receipt receipt = new Receipt(
                    ReceiptId.generate(),
                    PAYMENT_ID,
                    MERCHANT_ID,
                    null,
                    AMOUNT,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            assertThat(receipt.getMerchantName()).isEmpty();
        }
    }

    @Nested
    @DisplayName("voidReceipt")
    class VoidReceipt {

        @Test
        @DisplayName("ISSUED 상태면 VOIDED로 변경")
        void success() {
            Receipt receipt = createReceipt(ReceiptId.generate());

            receipt.voidReceipt();

            assertThat(receipt.getStatus()).isEqualTo(ReceiptStatus.VOIDED);
            assertThat(receipt.isVoided()).isTrue();
        }

        @Test
        @DisplayName("이미 VOIDED면 RECEIPT_ALREADY_VOIDED 예외")
        void alreadyVoided_throws() {
            Receipt receipt = createReceipt(ReceiptId.generate());
            receipt.voidReceipt();

            assertThatThrownBy(() -> receipt.voidReceipt())
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("이미 무효화된 영수증");
        }
    }
}
