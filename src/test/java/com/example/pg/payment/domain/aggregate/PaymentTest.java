package com.example.pg.payment.domain.aggregate;

import com.example.pg.payment.domain.enumerate.PaymentStatus;
import com.example.pg.payment.domain.vo.PaymentId;
import com.example.pg.common.exception.BusinessException;
import com.example.pg.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Payment 애그리거트")
class PaymentTest {

    private static final String PAYMENT_ID = "payment-1";
    private static final String MERCHANT_ID = "merchant-1";
    private static final long AMOUNT = 10_000L;
    private static final String MERCHANT_ORDER_ID = "ord-1";
    private static final String ORDER_NAME = "테스트 주문";
    private static final String CUSTOMER_EMAIL = "test@example.com";
    private static final String CUSTOMER_NAME = "홍길동";
    private static final String CALLBACK_URL = "https://merchant.com/callback";

    private static Payment readyPayment() {
        return new Payment(
                PaymentId.from(PAYMENT_ID), MERCHANT_ID, AMOUNT,
                MERCHANT_ORDER_ID, ORDER_NAME, CUSTOMER_EMAIL, CUSTOMER_NAME, CALLBACK_URL
        );
    }

    private static Payment authorizingPayment() {
        Payment p = readyPayment();
        p.startAuthorization();
        return p;
    }

    private static Payment authorizedPayment() {
        Payment p = authorizingPayment();
        p.authorizeSuccess("approval-123", "tx-456", LocalDateTime.now());
        return p;
    }

    @Nested
    @DisplayName("생성자")
    class Constructor {

        @Test
        @DisplayName("필수 필드로 생성 시 READY 상태")
        void createsWithReadyStatus() {
            Payment payment = readyPayment();

            assertThat(payment.getId()).isEqualTo(PAYMENT_ID);
            assertThat(payment.getMerchantId()).isEqualTo(MERCHANT_ID);
            assertThat(payment.getAmount()).isEqualTo(AMOUNT);
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.READY);
            assertThat(payment.getCardCompany()).isNull();
            assertThat(payment.getCreatedAt()).isNotNull();
            assertThat(payment.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("CardCompany와 함께 생성 시 연관관계 설정")
        void createsWithCardCompany() {
            CardCompany cardCompany = CardCompany.create("SHINHAN", "신한카드", "https://shinhan.com/");
            Payment payment = new Payment(
                    PaymentId.from(PAYMENT_ID), MERCHANT_ID, AMOUNT,
                    MERCHANT_ORDER_ID, ORDER_NAME, CUSTOMER_EMAIL, CUSTOMER_NAME, CALLBACK_URL,
                    cardCompany
            );

            assertThat(payment.getCardCompany()).isEqualTo(cardCompany);
            assertThat(payment.getCardCompany().getCode()).isEqualTo("SHINHAN");
        }
    }

    @Nested
    @DisplayName("startAuthorization")
    class StartAuthorization {

        @Test
        @DisplayName("READY면 AUTHORIZING으로 전이")
        void success() {
            Payment payment = readyPayment();
            payment.startAuthorization();
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.AUTHORIZING);
        }

        @Test
        @DisplayName("READY가 아니면 PAYMENT_INVALID_STATUS")
        void whenNotReady() {
            Payment payment = authorizingPayment();
            assertThatThrownBy(payment::startAuthorization)
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.PAYMENT_INVALID_STATUS);
        }
    }

    @Nested
    @DisplayName("authorizeSuccess")
    class AuthorizeSuccess {

        @Test
        @DisplayName("AUTHORIZING이면 AUTHORIZED로 전이하고 승인 정보 저장")
        void success() {
            Payment payment = authorizingPayment();
            String approvalNumber = "12345678";
            String transactionId = "tx-abc";
            LocalDateTime approvedAt = LocalDateTime.now();

            payment.authorizeSuccess(approvalNumber, transactionId, approvedAt);

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
            assertThat(payment.getApprovalNumber()).isEqualTo(approvalNumber);
            assertThat(payment.getTransactionId()).isEqualTo(transactionId);
            assertThat(payment.getApprovedAt()).isEqualTo(approvedAt);
        }

        @Test
        @DisplayName("AUTHORIZING이 아니면 PAYMENT_INVALID_STATUS")
        void whenNotAuthorizing() {
            Payment payment = readyPayment();
            assertThatThrownBy(() -> payment.authorizeSuccess("a", "t", LocalDateTime.now()))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.PAYMENT_INVALID_STATUS);
        }
    }

    @Nested
    @DisplayName("authorizeFail")
    class AuthorizeFail {

        @Test
        @DisplayName("AUTHORIZING이면 FAILED로 전이")
        void success() {
            Payment payment = authorizingPayment();
            payment.authorizeFail();
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        }

        @Test
        @DisplayName("AUTHORIZING이 아니면 PAYMENT_INVALID_STATUS")
        void whenNotAuthorizing() {
            Payment payment = readyPayment();
            assertThatThrownBy(payment::authorizeFail)
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.PAYMENT_INVALID_STATUS);
        }
    }

    @Nested
    @DisplayName("cancel")
    class Cancel {

        @Test
        @DisplayName("AUTHORIZED면 CANCELED로 전이")
        void success() {
            Payment payment = authorizedPayment();
            payment.cancel();
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
        }

        @Test
        @DisplayName("AUTHORIZED가 아니면 PAYMENT_INVALID_STATUS")
        void whenNotAuthorized() {
            Payment payment = authorizingPayment();
            assertThatThrownBy(payment::cancel)
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.PAYMENT_INVALID_STATUS);
        }
    }

    @Nested
    @DisplayName("markAsAborted")
    class MarkAsAborted {

        @Test
        @DisplayName("READY면 ABORTED로 전이")
        void success() {
            Payment payment = readyPayment();
            payment.markAsAborted();
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.ABORTED);
        }

        @Test
        @DisplayName("READY가 아니면 PAYMENT_INVALID_STATUS")
        void whenNotReady() {
            Payment payment = authorizingPayment();
            assertThatThrownBy(payment::markAsAborted)
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.PAYMENT_INVALID_STATUS);
        }
    }
}
