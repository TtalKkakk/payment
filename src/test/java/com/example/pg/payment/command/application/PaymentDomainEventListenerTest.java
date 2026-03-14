package com.example.pg.payment.command.application;

import com.example.pg.payment.domain.aggregate.Payment;
import com.example.pg.payment.domain.enumerate.PaymentStatus;
import com.example.pg.payment.domain.event.PaymentCreatedEvent;
import com.example.pg.payment.domain.event.PaymentStatusChangedEvent;
import com.example.pg.payment.domain.vo.PaymentId;
import com.example.pg.payment.infrastructure.persistence.PaymentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentDomainEventListener")
class PaymentDomainEventListenerTest {

    private static final String PAYMENT_ID = "payment-1";
    private static final String MERCHANT_ID = "merchant-1";
    private static final long AMOUNT = 10_000L;

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentWebhookService paymentWebhookService;

    @InjectMocks
    private PaymentDomainEventListener listener;

    @Nested
    @DisplayName("onPaymentCreated")
    class OnPaymentCreated {

        @Test
        @DisplayName("이벤트 수신 시 로그만 남김 (부수효과 없음)")
        void receivesEvent() {
            PaymentCreatedEvent event = PaymentCreatedEvent.from(PAYMENT_ID, MERCHANT_ID, AMOUNT);

            listener.onPaymentCreated(event);

            verify(paymentRepository, never()).load(any());
            verify(paymentWebhookService, never()).sendWebhook(any());
        }
    }

    @Nested
    @DisplayName("onPaymentStatusChanged")
    class OnPaymentStatusChanged {

        @Test
        @DisplayName("AUTHORIZED면 웹훅 발송 안 함 (영수증은 ReceiptOnPaymentApprovedListener에서 처리)")
        void authorizedNoWebhook() {
            PaymentStatusChangedEvent event = PaymentStatusChangedEvent.from(PAYMENT_ID, PaymentStatus.AUTHORIZED);

            listener.onPaymentStatusChanged(event);

            verify(paymentRepository, never()).load(any());
            verify(paymentWebhookService, never()).sendWebhook(any());
        }

        @Test
        @DisplayName("FAILED면 웹훅 발송")
        void failedSendsWebhook() {
            PaymentStatusChangedEvent event = PaymentStatusChangedEvent.from(PAYMENT_ID, PaymentStatus.FAILED);
            Payment payment = new Payment(
                    PaymentId.from(PAYMENT_ID), MERCHANT_ID, AMOUNT,
                    "ord-1", "주문", "a@a.com", "홍길동", "https://cb"
            );
            payment.startAuthorization();
            payment.authorizeFail();
            when(paymentRepository.load(PaymentId.from(PAYMENT_ID))).thenReturn(Optional.of(payment));

            listener.onPaymentStatusChanged(event);

            verify(paymentWebhookService).sendWebhook(any(Payment.class));
        }

        @Test
        @DisplayName("CANCELED면 웹훅 발송")
        void canceledSendsWebhook() {
            PaymentStatusChangedEvent event = PaymentStatusChangedEvent.from(PAYMENT_ID, PaymentStatus.CANCELED);
            Payment payment = new Payment(
                    PaymentId.from(PAYMENT_ID), MERCHANT_ID, AMOUNT,
                    "ord-1", "주문", "a@a.com", "홍길동", "https://cb"
            );
            payment.startAuthorization();
            payment.authorizeSuccess("a", "t", java.time.LocalDateTime.now());
            payment.cancel();
            when(paymentRepository.load(PaymentId.from(PAYMENT_ID))).thenReturn(Optional.of(payment));

            listener.onPaymentStatusChanged(event);

            verify(paymentWebhookService).sendWebhook(any(Payment.class));
        }

        @Test
        @DisplayName("AUTHORIZING 등 웹훅 대상이 아니면 발송 안 함")
        void otherStatusNoWebhook() {
            PaymentStatusChangedEvent event = PaymentStatusChangedEvent.from(PAYMENT_ID, PaymentStatus.AUTHORIZING);

            listener.onPaymentStatusChanged(event);

            verify(paymentRepository, never()).load(any());
            verify(paymentWebhookService, never()).sendWebhook(any());
        }

        @Test
        @DisplayName("FAILED/CANCELED 시 결제 없으면 웹훅 미호출")
        void paymentNotFound() {
            PaymentStatusChangedEvent event = PaymentStatusChangedEvent.from(PAYMENT_ID, PaymentStatus.FAILED);
            when(paymentRepository.load(PaymentId.from(PAYMENT_ID))).thenReturn(Optional.empty());

            listener.onPaymentStatusChanged(event);

            verify(paymentRepository).load(PaymentId.from(PAYMENT_ID));
            verify(paymentWebhookService, never()).sendWebhook(any());
        }
    }
}
