package com.example.pg.payment.command.application;

import com.example.pg.payment.domain.aggregate.CardCompany;
import com.example.pg.payment.domain.aggregate.Payment;
import com.example.pg.payment.domain.enumerate.PaymentStatus;
import com.example.pg.payment.domain.event.AuthorizationStartedEvent;
import com.example.pg.payment.domain.event.PaymentCreatedEvent;
import com.example.pg.payment.domain.event.PaymentStatusChangedEvent;
import com.example.pg.payment.domain.vo.PaymentId;
import com.example.pg.payment.infrastructure.persistence.CardCompanyRepository;
import com.example.pg.payment.infrastructure.persistence.PaymentRepository;
import com.example.pg.common.exception.BusinessException;
import com.example.pg.common.exception.ErrorCode;
import com.example.pg.payment.command.application.port.RefundPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService")
class PaymentServiceTest {

    private static final String MERCHANT_ID = "merchant-1";
    private static final String PAYMENT_ID = "payment-1";
    private static final long AMOUNT = 10_000L;
    private static final String BILLING_KEY = "bk-token-123";
    private static final String CARD_COMPANY_CODE = "SHINHAN";

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private CardCompanyRepository cardCompanyRepository;
    @Mock
    private RefundPort refundPort;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PaymentService paymentService;

    @Nested
    @DisplayName("createPayment")
    class CreatePayment {

        @Test
        @DisplayName("성공 시 READY 결제 저장 후 PaymentCreatedEvent 발행")
        void success() {
            when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

            PaymentId result = paymentService.createPayment(
                    MERCHANT_ID, AMOUNT, "ord-1", "주문", "a@a.com", "홍길동", "https://cb", null
            );

            assertThat(result).isNotNull();
            assertThat(result.getValue()).isNotBlank();

            ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(paymentCaptor.capture());
            assertThat(paymentCaptor.getValue().getStatus()).isEqualTo(PaymentStatus.READY);
            assertThat(paymentCaptor.getValue().getCardCompany()).isNull();

            ArgumentCaptor<PaymentCreatedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentCreatedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().amount()).isEqualTo(AMOUNT);
        }

        @Test
        @DisplayName("cardCompanyCode가 있으면 해당 카드사와 연관해 저장")
        void successWithCardCompany() {
            CardCompany cardCompany = CardCompany.create(CARD_COMPANY_CODE, "신한카드", "https://shinhan.com/");
            when(cardCompanyRepository.findByCode(CARD_COMPANY_CODE)).thenReturn(Optional.of(cardCompany));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

            paymentService.createPayment(
                    MERCHANT_ID, AMOUNT, "ord-1", "주문", "a@a.com", "홍길동", "https://cb", CARD_COMPANY_CODE
            );

            ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(paymentCaptor.capture());
            assertThat(paymentCaptor.getValue().getCardCompany()).isEqualTo(cardCompany);
        }

        @Test
        @DisplayName("amount가 0 이하면 PAYMENT_AMOUNT_INVALID")
        void invalidAmount() {
            assertThatThrownBy(() -> paymentService.createPayment(
                    MERCHANT_ID, 0, "ord-1", "주문", "a@a.com", "홍길동", "https://cb", null))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.PAYMENT_AMOUNT_INVALID);

            verify(paymentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("startAuthorization")
    class StartAuthorization {

        @Test
        @DisplayName("결제 없으면 PAYMENT_NOT_FOUND")
        void paymentNotFound() {
            when(paymentRepository.load(any(PaymentId.class))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.startAuthorization(PAYMENT_ID, BILLING_KEY))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.PAYMENT_NOT_FOUND);
        }

        @Test
        @DisplayName("billingKey가 blank면 BILLING_KEY_REQUIRED")
        void billingKeyRequired() {
            assertThatThrownBy(() -> paymentService.startAuthorization(PAYMENT_ID, "   "))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.BILLING_KEY_REQUIRED);
        }

        @Test
        @DisplayName("성공 시 상태 AUTHORIZING으로 변경 후 AuthorizationStartedEvent 발행")
        void success() {
            Payment payment = new Payment(
                    PaymentId.from(PAYMENT_ID), MERCHANT_ID, AMOUNT,
                    "ord-1", "주문", "a@a.com", "홍길동", "https://cb"
            );
            when(paymentRepository.load(PaymentId.from(PAYMENT_ID))).thenReturn(Optional.of(payment));

            paymentService.startAuthorization(PAYMENT_ID, BILLING_KEY);

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.AUTHORIZING);
            ArgumentCaptor<AuthorizationStartedEvent> eventCaptor = ArgumentCaptor.forClass(AuthorizationStartedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().paymentId()).isEqualTo(PAYMENT_ID);
            assertThat(eventCaptor.getValue().billingKey()).isEqualTo(BILLING_KEY);
        }
    }

    @Nested
    @DisplayName("compensateCreationFailure")
    class CompensateCreationFailure {

        @Test
        @DisplayName("READY 결제가 있으면 markAsAborted 호출")
        void success() {
            Payment payment = new Payment(
                    PaymentId.from(PAYMENT_ID), MERCHANT_ID, AMOUNT,
                    "ord-1", "주문", "a@a.com", "홍길동", "https://cb"
            );
            when(paymentRepository.findByIdAndStatus(PAYMENT_ID, PaymentStatus.READY))
                    .thenReturn(Optional.of(payment));

            paymentService.compensateCreationFailure(PAYMENT_ID);

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.ABORTED);
        }
    }

    @Nested
    @DisplayName("createPaymentAndStartAuthorization")
    class CreatePaymentAndStartAuthorization {

        @Test
        @DisplayName("billingKey 없으면 BILLING_KEY_REQUIRED")
        void billingKeyRequired() {
            assertThatThrownBy(() -> paymentService.createPaymentAndStartAuthorization(
                    MERCHANT_ID, AMOUNT, "ord-1", "주문", "a@a.com", "홍길동", "https://cb", null, null))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.BILLING_KEY_REQUIRED);
        }

        @Test
        @DisplayName("createPayment 실패 시 PAYMENT_CREATION_FAILED")
        void createFails() {
            when(paymentRepository.save(any(Payment.class))).thenThrow(new RuntimeException("DB error"));

            assertThatThrownBy(() -> paymentService.createPaymentAndStartAuthorization(
                    MERCHANT_ID, AMOUNT, "ord-1", "주문", "a@a.com", "홍길동", "https://cb", BILLING_KEY, null))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.PAYMENT_CREATION_FAILED);
        }

        @Test
        @DisplayName("startAuthorization 실패 시 보상 호출 후 AUTHORIZATION_START_FAILED")
        void startAuthFailsThenCompensate() {
            java.util.List<Payment> saved = new java.util.ArrayList<>();
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
                Payment p = inv.getArgument(0);
                saved.add(p);
                return p;
            });
            when(paymentRepository.load(any(PaymentId.class))).thenThrow(new RuntimeException("network error"));
            when(paymentRepository.findByIdAndStatus(anyString(), eq(PaymentStatus.READY))).thenAnswer(inv -> {
                String id = inv.getArgument(0);
                if (saved.isEmpty()) return Optional.empty();
                Payment original = saved.get(0);
                if (!original.getId().equals(id)) return Optional.empty();
                Payment readyPayment = new Payment(
                        PaymentId.from(id), original.getMerchantId(), original.getAmount(),
                        original.getMerchantOrderId(), original.getOrderName(), original.getCustomerEmail(),
                        original.getCustomerName(), original.getCallbackUrl(), original.getCardCompany()
                );
                return Optional.of(readyPayment);
            });

            assertThatThrownBy(() -> paymentService.createPaymentAndStartAuthorization(
                    MERCHANT_ID, AMOUNT, "ord-1", "주문", "a@a.com", "홍길동", "https://cb", BILLING_KEY, null))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.AUTHORIZATION_START_FAILED);

            assertThat(saved).hasSize(1);
            verify(paymentRepository).findByIdAndStatus(anyString(), eq(PaymentStatus.READY));
        }
    }

    @Nested
    @DisplayName("cancelPayment")
    class CancelPayment {

        @Test
        @DisplayName("결제 없으면 PAYMENT_NOT_FOUND")
        void paymentNotFound() {
            when(paymentRepository.load(PaymentId.from(PAYMENT_ID))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.cancelPayment(MERCHANT_ID, PAYMENT_ID))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.PAYMENT_NOT_FOUND);
        }

        @Test
        @DisplayName("다른 가맹점 결제면 PAYMENT_INVALID_STATUS")
        void wrongMerchant() {
            Payment payment = new Payment(
                    PaymentId.from(PAYMENT_ID), "other-merchant", AMOUNT,
                    "ord-1", "주문", "a@a.com", "홍길동", "https://cb"
            );
            payment.startAuthorization();
            payment.authorizeSuccess("a", "t", java.time.LocalDateTime.now());
            when(paymentRepository.load(PaymentId.from(PAYMENT_ID))).thenReturn(Optional.of(payment));

            assertThatThrownBy(() -> paymentService.cancelPayment(MERCHANT_ID, PAYMENT_ID))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.PAYMENT_INVALID_STATUS);

            verify(refundPort, never()).requestRefund(any(), anyLong());
        }

        @Test
        @DisplayName("환불 요청 실패 시 PAYMENT_INVALID_STATUS")
        void refundFails() {
            Payment payment = new Payment(
                    PaymentId.from(PAYMENT_ID), MERCHANT_ID, AMOUNT,
                    "ord-1", "주문", "a@a.com", "홍길동", "https://cb"
            );
            payment.startAuthorization();
            payment.authorizeSuccess("a", "t", java.time.LocalDateTime.now());
            when(paymentRepository.load(PaymentId.from(PAYMENT_ID))).thenReturn(Optional.of(payment));
            when(refundPort.requestRefund(PAYMENT_ID, AMOUNT)).thenReturn(false);

            assertThatThrownBy(() -> paymentService.cancelPayment(MERCHANT_ID, PAYMENT_ID))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.PAYMENT_INVALID_STATUS);
        }

        @Test
        @DisplayName("성공 시 cancel 후 PaymentStatusChangedEvent 발행")
        void success() {
            Payment payment = new Payment(
                    PaymentId.from(PAYMENT_ID), MERCHANT_ID, AMOUNT,
                    "ord-1", "주문", "a@a.com", "홍길동", "https://cb"
            );
            payment.startAuthorization();
            payment.authorizeSuccess("a", "t", java.time.LocalDateTime.now());
            when(paymentRepository.load(PaymentId.from(PAYMENT_ID))).thenReturn(Optional.of(payment));
            when(refundPort.requestRefund(PAYMENT_ID, AMOUNT)).thenReturn(true);

            paymentService.cancelPayment(MERCHANT_ID, PAYMENT_ID);

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
            ArgumentCaptor<PaymentStatusChangedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentStatusChangedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().paymentId()).isEqualTo(PAYMENT_ID);
            assertThat(eventCaptor.getValue().status()).isEqualTo(PaymentStatus.CANCELED);
        }
    }
}
