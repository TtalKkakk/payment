package com.example.pg.receipt.query.application;

import com.example.pg.receipt.domain.aggregate.Receipt;
import com.example.pg.receipt.domain.vo.ReceiptId;
import com.example.pg.receipt.infrastructure.persistence.ReceiptRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReceiptQueryService")
class ReceiptQueryServiceTest {

    private static final String PAYMENT_ID = "pay-001";

    @Mock
    private ReceiptRepository receiptRepository;

    @InjectMocks
    private ReceiptQueryService receiptQueryService;

    @Nested
    @DisplayName("getByPaymentId")
    class GetByPaymentId {

        @Test
        @DisplayName("영수증이 없으면 empty")
        void returnsEmptyWhenNotFound() {
            when(receiptRepository.findByPaymentId(PAYMENT_ID)).thenReturn(Optional.empty());

            Optional<Receipt> result = receiptQueryService.getByPaymentId(PAYMENT_ID);

            assertThat(result).isEmpty();
            verify(receiptRepository).findByPaymentId(PAYMENT_ID);
        }

        @Test
        @DisplayName("영수증이 있으면 반환")
        void returnsReceiptWhenFound() {
            Receipt receipt = new Receipt(
                    ReceiptId.generate(),
                    PAYMENT_ID,
                    "merchant-1",
                    "테스트 가맹점",
                    10_000L,
                    "주문명",
                    "ord-1",
                    "홍길동",
                    "approval-1",
                    "tx-1",
                    LocalDateTime.now()
            );
            when(receiptRepository.findByPaymentId(PAYMENT_ID)).thenReturn(Optional.of(receipt));

            Optional<Receipt> result = receiptQueryService.getByPaymentId(PAYMENT_ID);

            assertThat(result).containsSame(receipt);
            verify(receiptRepository).findByPaymentId(PAYMENT_ID);
            verifyNoMoreInteractions(receiptRepository);
        }
    }
}

