package com.example.pg.payment.command.application;

import com.example.pg.merchant.domain.aggregate.Merchant;
import com.example.pg.payment.domain.aggregate.Payment;
import com.example.pg.payment.domain.vo.PaymentId;
import com.example.pg.merchant.infrastructure.persistence.MerchantRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentWebhookService")
class PaymentWebhookServiceTest {

    private static final String PAYMENT_ID = "payment-1";
    private static final String MERCHANT_ID = "merchant-1";

    @Mock
    private MerchantRepository merchantRepository;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private ObjectMapper objectMapper;

    private PaymentWebhookService paymentWebhookService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        paymentWebhookService = new PaymentWebhookService(merchantRepository, restTemplate, objectMapper);
    }

    @Nested
    @DisplayName("sendWebhook")
    class SendWebhook {

        @Test
        @DisplayName("callbackUrl이 null이면 발송 생략")
        void noCallbackUrl() {
            Payment payment = new Payment(
                    PaymentId.from(PAYMENT_ID), MERCHANT_ID, 10_000L,
                    "ord-1", "주문", "a@a.com", "홍길동", null
            );
            payment.startAuthorization();
            payment.authorizeSuccess("a", "t", java.time.LocalDateTime.now());

            paymentWebhookService.sendWebhook(payment);

            verify(restTemplate, never()).exchange(anyString(), any(), any(), eq(String.class));
        }

        @Test
        @DisplayName("callbackUrl이 blank면 발송 생략")
        void blankCallbackUrl() {
            Payment payment = new Payment(
                    PaymentId.from(PAYMENT_ID), MERCHANT_ID, 10_000L,
                    "ord-1", "주문", "a@a.com", "홍길동", "   "
            );
            payment.startAuthorization();
            payment.authorizeFail();

            paymentWebhookService.sendWebhook(payment);

            verify(restTemplate, never()).exchange(anyString(), any(), any(), eq(String.class));
        }

        @Test
        @DisplayName("callbackUrl 있으면 POST 발송")
        void sendsPost() throws Exception {
            Payment payment = new Payment(
                    PaymentId.from(PAYMENT_ID), MERCHANT_ID, 10_000L,
                    "ord-1", "주문", "a@a.com", "홍길동", "https://merchant.com/webhook"
            );
            payment.startAuthorization();
            payment.authorizeSuccess("appr-1", "tx-1", java.time.LocalDateTime.now());
            when(merchantRepository.findById(MERCHANT_ID)).thenReturn(Optional.of(
                    new Merchant("m1", "pk", "sk_secret", "가맹점", "app-1", com.example.pg.merchant.domain.enumerate.MerchantStatus.ACTIVE)
            ));
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"paymentId\":\"payment-1\"}");
            when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
                    .thenReturn(ResponseEntity.ok("ok"));

            paymentWebhookService.sendWebhook(payment);

            verify(restTemplate).exchange(eq("https://merchant.com/webhook"), any(), any(), eq(String.class));
        }
    }
}
