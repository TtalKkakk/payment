package com.example.pg.receipt.command.application;

import com.example.pg.common.exception.BusinessException;
import com.example.pg.merchant.presentation.port.MerchantPort;
import com.example.pg.payment.presentation.port.PaymentPort;
import com.example.pg.payment.presentation.port.dto.PaymentSnapshotForReceiptDto;
import com.example.pg.receipt.domain.aggregate.Receipt;
import com.example.pg.receipt.domain.vo.ReceiptId;
import com.example.pg.receipt.infrastructure.persistence.ReceiptRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReceiptService")
class ReceiptServiceTest {

    private static final String PAYMENT_ID = "pay-001";
    private static final String MERCHANT_ID = "merchant-1";
    private static final String MERCHANT_NAME = "테스트 가맹점";

    private static PaymentSnapshotForReceiptDto authorizedSnapshot() {
        return new PaymentSnapshotForReceiptDto(
                PAYMENT_ID,
                MERCHANT_ID,
                10_000L,
                "주문명",
                "ord-1",
                "홍길동",
                "approval-1",
                "tx-1",
                LocalDateTime.now()
        );
    }

    @Mock
    private PaymentPort paymentPort;
    @Mock
    private MerchantPort merchantPort;
    @Mock
    private ReceiptRepository receiptRepository;

    @InjectMocks
    private ReceiptService receiptService;

    @Nested
    @DisplayName("issueForPayment")
    class IssueForPayment {

        @Test
        @DisplayName("결제 없으면 PAYMENT_NOT_FOUND")
        void paymentNotFound_throws() {
            when(paymentPort.existsPayment(PAYMENT_ID)).thenReturn(false);

            assertThatThrownBy(() -> receiptService.issueForPayment(PAYMENT_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("결제를 찾을 수 없습니다");
        }

        @Test
        @DisplayName("승인 완료 결제가 아니면 RECEIPT_CANNOT_ISSUE")
        void notAuthorized_throws() {
            when(paymentPort.existsPayment(PAYMENT_ID)).thenReturn(true);
            when(paymentPort.findAuthorizedPayment(PAYMENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> receiptService.issueForPayment(PAYMENT_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("승인 완료된 결제에만");
        }

        @Test
        @DisplayName("이미 영수증 있으면 기존 반환 (멱등)")
        void idempotent_returnsExisting() {
            when(paymentPort.existsPayment(PAYMENT_ID)).thenReturn(true);
            when(paymentPort.findAuthorizedPayment(PAYMENT_ID)).thenReturn(Optional.of(authorizedSnapshot()));
            Receipt existingReceipt = new Receipt(
                    ReceiptId.generate(),
                    PAYMENT_ID,
                    MERCHANT_ID,
                    MERCHANT_NAME,
                    10_000L,
                    "주문명",
                    "ord-1",
                    "홍길동",
                    "approval-1",
                    "tx-1",
                    LocalDateTime.now()
            );
            when(receiptRepository.findByPaymentId(PAYMENT_ID)).thenReturn(Optional.of(existingReceipt));

            Receipt result = receiptService.issueForPayment(PAYMENT_ID);

            assertThat(result).isSameAs(existingReceipt);
            verify(receiptRepository, never()).save(any());
            verify(merchantPort, never()).getMerchantName(any());
        }

        @Test
        @DisplayName("승인 완료 결제면 영수증 생성 후 저장")
        void success_createsAndSavesReceipt() {
            when(paymentPort.existsPayment(PAYMENT_ID)).thenReturn(true);
            when(paymentPort.findAuthorizedPayment(PAYMENT_ID)).thenReturn(Optional.of(authorizedSnapshot()));
            when(receiptRepository.findByPaymentId(PAYMENT_ID)).thenReturn(Optional.empty());
            when(merchantPort.getMerchantName(MERCHANT_ID)).thenReturn(MERCHANT_NAME);
            when(receiptRepository.save(any(Receipt.class))).thenAnswer(inv -> inv.getArgument(0));

            Receipt result = receiptService.issueForPayment(PAYMENT_ID);

            assertThat(result).isNotNull();
            assertThat(result.getPaymentId()).isEqualTo(PAYMENT_ID);
            assertThat(result.getMerchantId()).isEqualTo(MERCHANT_ID);
            assertThat(result.getMerchantName()).isEqualTo(MERCHANT_NAME);
            assertThat(result.getAmount()).isEqualTo(10_000L);
            assertThat(result.getOrderName()).isEqualTo("주문명");
            assertThat(result.getCustomerName()).isEqualTo("홍길동");
            assertThat(result.getApprovalNumber()).isEqualTo("approval-1");
            assertThat(result.getTransactionId()).isEqualTo("tx-1");
            assertThat(result.isVoided()).isFalse();

            ArgumentCaptor<Receipt> captor = ArgumentCaptor.forClass(Receipt.class);
            verify(receiptRepository).save(captor.capture());
            assertThat(captor.getValue().getReceiptNumber()).startsWith("RCP-");
            verify(merchantPort).getMerchantName(MERCHANT_ID);
        }

        @Test
        @DisplayName("가맹점명은 MerchantPort에서 조회해 반영")
        void merchantNameFromPort() {
            when(paymentPort.existsPayment(PAYMENT_ID)).thenReturn(true);
            when(paymentPort.findAuthorizedPayment(PAYMENT_ID)).thenReturn(Optional.of(authorizedSnapshot()));
            when(receiptRepository.findByPaymentId(PAYMENT_ID)).thenReturn(Optional.empty());
            when(merchantPort.getMerchantName(MERCHANT_ID)).thenReturn("다른 가맹점명");
            when(receiptRepository.save(any(Receipt.class))).thenAnswer(inv -> inv.getArgument(0));

            Receipt result = receiptService.issueForPayment(PAYMENT_ID);

            assertThat(result.getMerchantName()).isEqualTo("다른 가맹점명");
        }
    }
}
