package com.example.pg.receipt.command.application;

import com.example.pg.common.exception.BusinessException;
import com.example.pg.common.exception.ErrorCode;
import com.example.pg.receipt.command.application.port.ReceiptPdfPort;
import com.example.pg.receipt.domain.aggregate.Receipt;
import com.example.pg.receipt.domain.vo.ReceiptId;
import com.example.pg.receipt.query.application.ReceiptQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReceiptPdfService")
class ReceiptPdfServiceTest {

    private static final String PAYMENT_ID = "pay-001";
    private static final String MERCHANT_ID = "merchant-1";
    private static final String OTHER_MERCHANT_ID = "merchant-2";

    private static Receipt createReceipt(String merchantId) {
        return new Receipt(
                ReceiptId.generate(),
                PAYMENT_ID,
                merchantId,
                "테스트 가맹점",
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
    private ReceiptQueryService receiptQueryService;
    @Mock
    private ReceiptPdfPort receiptPdfPort;

    @InjectMocks
    private ReceiptPdfService receiptPdfService;

    @Nested
    @DisplayName("generateByPaymentId")
    class GenerateByPaymentId {

        @Test
        @DisplayName("영수증이 없으면 RECEIPT_NOT_FOUND")
        void receiptNotFound_throws() {
            when(receiptQueryService.getByPaymentId(PAYMENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> receiptPdfService.generateByPaymentId(PAYMENT_ID, MERCHANT_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.RECEIPT_NOT_FOUND));

            verify(receiptQueryService).getByPaymentId(PAYMENT_ID);
            verifyNoInteractions(receiptPdfPort);
        }

        @Test
        @DisplayName("다른 가맹점 소유 영수증이면 RECEIPT_NOT_FOUND")
        void otherMerchant_throws() {
            Receipt receipt = createReceipt(MERCHANT_ID);
            when(receiptQueryService.getByPaymentId(PAYMENT_ID)).thenReturn(Optional.of(receipt));

            assertThatThrownBy(() -> receiptPdfService.generateByPaymentId(PAYMENT_ID, OTHER_MERCHANT_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.RECEIPT_NOT_FOUND));

            verify(receiptQueryService).getByPaymentId(PAYMENT_ID);
            verifyNoInteractions(receiptPdfPort);
        }

        @Test
        @DisplayName("같은 가맹점이면 PDF 생성 후 반환")
        void sameMerchant_returnsPdfBytes() {
            Receipt receipt = createReceipt(MERCHANT_ID);
            byte[] expectedPdf = new byte[]{1, 2, 3};
            when(receiptQueryService.getByPaymentId(PAYMENT_ID)).thenReturn(Optional.of(receipt));
            when(receiptPdfPort.generate(receipt)).thenReturn(expectedPdf);

            byte[] result = receiptPdfService.generateByPaymentId(PAYMENT_ID, MERCHANT_ID);

            assertThat(result).isSameAs(expectedPdf);
            verify(receiptQueryService).getByPaymentId(PAYMENT_ID);
            verify(receiptPdfPort).generate(receipt);
        }
    }
}
