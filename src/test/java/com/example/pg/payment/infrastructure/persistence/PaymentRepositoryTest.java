package com.example.pg.payment.infrastructure.persistence;

import com.example.pg.payment.domain.aggregate.Payment;
import com.example.pg.payment.domain.enumerate.PaymentStatus;
import com.example.pg.payment.domain.vo.PaymentId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.profiles.active=test")
@ActiveProfiles("test")
class PaymentRepositoryTest {

    @Autowired
    PaymentRepository paymentRepository;

    private Payment createPayment(String id, String merchantId, PaymentStatus status) {
        Payment payment = new Payment(
                PaymentId.from(id),
                merchantId,
                10_000L,
                "order-1",
                "테스트 주문",
                "test@example.com",
                "홍길동",
                "https://example.com/callback",
                null
        );
        if (status != PaymentStatus.READY) {
            payment.startAuthorization();
            if (status == PaymentStatus.AUTHORIZED) {
                payment.authorizeSuccess("approval-1", "tx-1", null);
            } else if (status == PaymentStatus.FAILED) {
                payment.authorizeFail();
            } else if (status == PaymentStatus.ABORTED) {
                payment.markAsAborted();
            }
        }
        return payment;
    }

    @Nested
    @DisplayName("save and findById")
    class SaveAndFindById {

        @Test
        @DisplayName("저장 후 findById로 조회된다")
        void saveAndFindById() {
            String id = "pay-001";
            Payment payment = createPayment(id, "merchant-1", PaymentStatus.READY);
            paymentRepository.save(payment);

            Optional<Payment> found = paymentRepository.findById(id);
            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(id);
            assertThat(found.get().getMerchantId()).isEqualTo("merchant-1");
            assertThat(found.get().getStatus()).isEqualTo(PaymentStatus.READY);
        }

        @Test
        @DisplayName("없는 id로 조회 시 empty 반환")
        void findById_empty() {
            Optional<Payment> found = paymentRepository.findById("no-such-id");
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("load(PaymentId)")
    class LoadByPaymentId {

        @Test
        @DisplayName("paymentId로 조회된다")
        void load_found() {
            String id = "pay-load-1";
            paymentRepository.save(createPayment(id, "merchant-1", PaymentStatus.READY));

            Optional<Payment> found = paymentRepository.load(PaymentId.from(id));
            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(id);
        }

        @Test
        @DisplayName("없는 paymentId로 조회 시 empty 반환")
        void load_empty() {
            Optional<Payment> found = paymentRepository.load(PaymentId.from("no-such"));
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByMerchantIdAndId")
    class FindByMerchantIdAndId {

        @Test
        @DisplayName("가맹점 id와 결제 id로 조회된다")
        void found() {
            String id = "pay-mid-1";
            String merchantId = "merchant-A";
            paymentRepository.save(createPayment(id, merchantId, PaymentStatus.READY));

            Optional<Payment> found = paymentRepository.findByMerchantIdAndId(merchantId, id);
            assertThat(found).isPresent();
            assertThat(found.get().getMerchantId()).isEqualTo(merchantId);
            assertThat(found.get().getId()).isEqualTo(id);
        }

        @Test
        @DisplayName("다른 가맹점 id로 조회 시 empty")
        void wrongMerchant_empty() {
            String id = "pay-mid-2";
            paymentRepository.save(createPayment(id, "merchant-A", PaymentStatus.READY));

            Optional<Payment> found = paymentRepository.findByMerchantIdAndId("other-merchant", id);
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByIdAndStatus")
    class FindByIdAndStatus {

        @Test
        @DisplayName("id와 status로 조회된다")
        void found() {
            String id = "pay-status-1";
            paymentRepository.save(createPayment(id, "merchant-1", PaymentStatus.READY));

            Optional<Payment> found = paymentRepository.findByIdAndStatus(id, PaymentStatus.READY);
            assertThat(found).isPresent();
            assertThat(found.get().getStatus()).isEqualTo(PaymentStatus.READY);
        }

        @Test
        @DisplayName("status가 다르면 empty")
        void wrongStatus_empty() {
            String id = "pay-status-2";
            paymentRepository.save(createPayment(id, "merchant-1", PaymentStatus.AUTHORIZED));

            Optional<Payment> found = paymentRepository.findByIdAndStatus(id, PaymentStatus.READY);
            assertThat(found).isEmpty();
        }
    }
}
