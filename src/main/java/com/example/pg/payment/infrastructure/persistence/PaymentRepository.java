package com.example.pg.payment.infrastructure.persistence;

import com.example.pg.payment.domain.aggregate.Payment;
import com.example.pg.payment.domain.enumerate.PaymentStatus;
import com.example.pg.payment.domain.vo.PaymentId;
import com.example.pg.payment.domain.vo.PaymentMerchantId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA 리포지토리.
 * 서비스 로직(결제 생성/조회/승인/보상/취소)에 맞춘 조회 메서드를 제공한다.
 */
public interface PaymentRepository extends JpaRepository<Payment, String> {

    /**
     * paymentId로 단건 조회. (startAuthorization, startCancellation, processAuthorization, 웹훅 발송 등)
     */
    default Optional<Payment> load(PaymentId paymentId) {
        return findById(paymentId.getValue());
    }

    /**
     * 가맹점별 결제 단건 조회. (GET /payments/{id} - 해당 가맹점 소유만 허용)
     */
    Optional<Payment> findByMerchantIdAndId(PaymentMerchantId merchantId, String id);

    /**
     * id + status로 조회. (보상: READY인 결제만 ABORTED 처리)
     */
    Optional<Payment> findByIdAndStatus(String id, PaymentStatus status);
}
