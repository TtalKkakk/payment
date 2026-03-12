package com.example.pg.payment.query.application;

import com.example.pg.payment.domain.aggregate.Payment;
import com.example.pg.payment.domain.vo.PaymentId;
import com.example.pg.payment.infrastructure.persistence.PaymentRepository;
import com.example.pg.payment.presentation.dto.PaymentDetailResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentQueryService")
class PaymentQueryServiceTest {

    private static final String MERCHANT_ID = "merchant-1";
    private static final String PAYMENT_ID = "payment-1";

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentQueryService paymentQueryService;

    @Nested
    @DisplayName("getPayment")
    class GetPayment {

        @Test
        @DisplayName("해당 가맹점 결제 있으면 PaymentDetailResponse 반환")
        void success() {
            Payment payment = new Payment(
                    PaymentId.from(PAYMENT_ID), MERCHANT_ID, 10_000L,
                    "ord-1", "주문", "a@a.com", "홍길동", "https://cb"
            );
            when(paymentRepository.findByMerchantIdAndId(MERCHANT_ID, PAYMENT_ID)).thenReturn(Optional.of(payment));

            Optional<PaymentDetailResponse> result = paymentQueryService.getPayment(MERCHANT_ID, PAYMENT_ID);

            assertThat(result).isPresent();
            assertThat(result.get().paymentId()).isEqualTo(PAYMENT_ID);
            assertThat(result.get().merchantId()).isEqualTo(MERCHANT_ID);
            assertThat(result.get().amount()).isEqualTo(10_000L);
            verify(paymentRepository).findByMerchantIdAndId(MERCHANT_ID, PAYMENT_ID);
        }

        @Test
        @DisplayName("결제 없거나 다른 가맹점이면 empty")
        void empty() {
            when(paymentRepository.findByMerchantIdAndId(MERCHANT_ID, PAYMENT_ID)).thenReturn(Optional.empty());

            Optional<PaymentDetailResponse> result = paymentQueryService.getPayment(MERCHANT_ID, PAYMENT_ID);

            assertThat(result).isEmpty();
        }
    }
}
