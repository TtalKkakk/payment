package com.example.pg.payment.command.application;

import com.example.pg.payment.domain.repository.CardCompanyPortRegistry;
import com.example.pg.payment.presentation.CardCompanyConnect;
import com.example.pg.payment.presentation.dto.PaymentApproveResponse;
import com.example.pg.payment.domain.aggregate.CardCompany;
import com.example.pg.payment.domain.aggregate.Payment;
import com.example.pg.payment.domain.enumerate.PaymentStatus;
import com.example.pg.payment.domain.event.PaymentStatusChangedEvent;
import com.example.pg.payment.domain.vo.PaymentId;
import com.example.pg.payment.infrastructure.persistence.PaymentRepository;
import com.example.pg.common.exception.BusinessException;
import com.example.pg.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentAuthorizationProcessor")
class PaymentAuthorizationProcessorTest {

    private static final String PAYMENT_ID = "payment-1";
    private static final String MERCHANT_ID = "merchant-1";
    private static final long AMOUNT = 10_000L;
    private static final String BILLING_KEY = "bk-123";
    private static final String CARD_COMPANY_CODE = "SHINHAN";

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private CardCompanyPortRegistry portRegistry;
    @Mock
    private CardCompanyConnect cardCompanyConnect;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PaymentAuthorizationProcessor processor;

    @Nested
    @DisplayName("processAuthorization")
    class ProcessAuthorization {

        @Test
        @DisplayName("결제가 없으면 아무 동작 없음")
        void paymentNotFound() {
            when(paymentRepository.load(PaymentId.from(PAYMENT_ID))).thenReturn(Optional.empty());

            processor.processAuthorization(PAYMENT_ID, BILLING_KEY);

            verify(portRegistry, never()).getPortOrThrow(any());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("결제 상태가 AUTHORIZING이 아니면 아무 동작 없음")
        void statusNotAuthorizing() {
            Payment payment = new Payment(
                    PaymentId.from(PAYMENT_ID), MERCHANT_ID, AMOUNT,
                    "ord-1", "주문", "a@a.com", "홍길동", "https://cb"
            );
            when(paymentRepository.load(PaymentId.from(PAYMENT_ID))).thenReturn(Optional.of(payment));

            processor.processAuthorization(PAYMENT_ID, BILLING_KEY);

            verify(portRegistry, never()).getPortOrThrow(any());
        }

        @Test
        @DisplayName("결제에 카드사가 없으면 PAYMENT_INVALID_STATUS")
        void noCardCompany() {
            Payment payment = new Payment(
                    PaymentId.from(PAYMENT_ID), MERCHANT_ID, AMOUNT,
                    "ord-1", "주문", "a@a.com", "홍길동", "https://cb", null
            );
            payment.startAuthorization();
            when(paymentRepository.load(PaymentId.from(PAYMENT_ID))).thenReturn(Optional.of(payment));

            assertThatThrownBy(() -> processor.processAuthorization(PAYMENT_ID, BILLING_KEY))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.PAYMENT_INVALID_STATUS);

            verify(portRegistry, never()).getPortOrThrow(any());
        }

        @Test
        @DisplayName("승인 성공 시 authorizeSuccess·save·이벤트 발행")
        void success() {
            CardCompany cardCompany = CardCompany.create(CARD_COMPANY_CODE, "신한", "https://shinhan.com/");
            Payment payment = new Payment(
                    PaymentId.from(PAYMENT_ID), MERCHANT_ID, AMOUNT,
                    "ord-1", "주문", "a@a.com", "홍길동", "https://cb", cardCompany
            );
            payment.startAuthorization();
            when(paymentRepository.load(PaymentId.from(PAYMENT_ID))).thenReturn(Optional.of(payment));
            when(portRegistry.getPortOrThrow(CARD_COMPANY_CODE)).thenReturn(cardCompanyConnect);
            when(cardCompanyConnect.approve(eq(PAYMENT_ID), eq(AMOUNT), eq(BILLING_KEY)))
                    .thenReturn(PaymentApproveResponse.success(PAYMENT_ID, "approval-123", "tx-456", LocalDateTime.now()));

            processor.processAuthorization(PAYMENT_ID, BILLING_KEY);

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
            assertThat(payment.getApprovalNumber()).isEqualTo("approval-123");
            assertThat(payment.getTransactionId()).isEqualTo("tx-456");
            verify(paymentRepository).save(payment);
            ArgumentCaptor<PaymentStatusChangedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentStatusChangedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().paymentId()).isEqualTo(PAYMENT_ID);
            assertThat(eventCaptor.getValue().status()).isEqualTo(PaymentStatus.AUTHORIZED);
        }

        @Test
        @DisplayName("승인 실패 시 authorizeFail·이벤트 발행")
        void failure() {
            CardCompany cardCompany = CardCompany.create(CARD_COMPANY_CODE, "신한", "https://shinhan.com/");
            Payment payment = new Payment(
                    PaymentId.from(PAYMENT_ID), MERCHANT_ID, AMOUNT,
                    "ord-1", "주문", "a@a.com", "홍길동", "https://cb", cardCompany
            );
            payment.startAuthorization();
            when(paymentRepository.load(PaymentId.from(PAYMENT_ID))).thenReturn(Optional.of(payment));
            when(portRegistry.getPortOrThrow(CARD_COMPANY_CODE)).thenReturn(cardCompanyConnect);
            when(cardCompanyConnect.approve(eq(PAYMENT_ID), eq(AMOUNT), eq(BILLING_KEY)))
                    .thenReturn(PaymentApproveResponse.failure(PAYMENT_ID, "E001", "잔액 부족"));

            processor.processAuthorization(PAYMENT_ID, BILLING_KEY);

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
            verify(paymentRepository, never()).save(any());
            ArgumentCaptor<PaymentStatusChangedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentStatusChangedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().paymentId()).isEqualTo(PAYMENT_ID);
            assertThat(eventCaptor.getValue().status()).isEqualTo(PaymentStatus.FAILED);
        }
    }
}
