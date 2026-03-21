package com.example.pg.payment.command.application;

import com.example.pg.common.exception.BusinessException;
import com.example.pg.common.exception.ErrorCode;
import com.example.pg.payment.domain.vo.PaymentId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentCreationOrchestratorService")
class PaymentCreationOrchestratorServiceTest {

    private static final String MERCHANT_ID = "merchant-1";
    private static final long AMOUNT = 10_000L;
    private static final String BILLING_KEY = "bk-token-123";

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentCreationOrchestratorService orchestrator;

    private PaymentId invokeCreate() {
        return orchestrator.createPaymentAndStartAuthorization(
                MERCHANT_ID, AMOUNT, "ord-1", "주문", "a@a.com", "홍길동", "https://cb", BILLING_KEY, null);
    }

    @Nested
    @DisplayName("createPaymentAndStartAuthorization")
    class CreatePaymentAndStartAuthorization {

        @Test
        @DisplayName("billingKey 없으면 BILLING_KEY_REQUIRED")
        void billingKeyRequired() {
            assertThatThrownBy(() -> orchestrator.createPaymentAndStartAuthorization(
                    MERCHANT_ID, AMOUNT, "ord-1", "주문", "a@a.com", "홍길동", "https://cb", null, null))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.BILLING_KEY_REQUIRED);

            verify(paymentService, never()).createPayment(
                    anyString(), anyLong(), anyString(), anyString(), anyString(), anyString(), anyString(), isNull());
        }

        @Test
        @DisplayName("amount가 0 이하면 PAYMENT_AMOUNT_INVALID")
        void invalidAmount() {
            assertThatThrownBy(() -> orchestrator.createPaymentAndStartAuthorization(
                    MERCHANT_ID, 0, "ord-1", "주문", "a@a.com", "홍길동", "https://cb", BILLING_KEY, null))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.PAYMENT_AMOUNT_INVALID);

            verify(paymentService, never()).createPayment(
                    anyString(), anyLong(), anyString(), anyString(), anyString(), anyString(), anyString(), isNull());
        }

        @Test
        @DisplayName("createPayment 실패 시 PAYMENT_CREATION_FAILED")
        void createFails() {
            when(paymentService.createPayment(
                    eq(MERCHANT_ID), eq(AMOUNT), eq("ord-1"), eq("주문"),
                    eq("a@a.com"), eq("홍길동"), eq("https://cb"), isNull()))
                    .thenThrow(new RuntimeException("DB error"));

            assertThatThrownBy(PaymentCreationOrchestratorServiceTest.this::invokeCreate)
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.PAYMENT_CREATION_FAILED);
        }

        @Test
        @DisplayName("startAuthorization 실패 시 보상 호출 후 AUTHORIZATION_START_FAILED")
        void startAuthFailsThenCompensate() {
            PaymentId pid = PaymentId.generate();
            when(paymentService.createPayment(
                    eq(MERCHANT_ID), eq(AMOUNT), eq("ord-1"), eq("주문"),
                    eq("a@a.com"), eq("홍길동"), eq("https://cb"), isNull()))
                    .thenReturn(pid);
            doThrow(new RuntimeException("network error"))
                    .when(paymentService).startAuthorization(pid.getValue(), BILLING_KEY);

            assertThatThrownBy(PaymentCreationOrchestratorServiceTest.this::invokeCreate)
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.AUTHORIZATION_START_FAILED);

            verify(paymentService).compensateCreationFailure(pid.getValue());
        }
    }
}
