package com.example.pg.receipt.infrastructure.persistence;

import com.example.pg.receipt.domain.aggregate.Receipt;
import com.example.pg.receipt.domain.vo.ReceiptId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 영수증 Spring Data JPA 리포지토리.
 */
public interface ReceiptRepository extends JpaRepository<Receipt, String> {

    default Optional<Receipt> load(ReceiptId receiptId) {
        return findById(receiptId.getValue());
    }

    Optional<Receipt> findByPaymentId(String paymentId);

    boolean existsByPaymentId(String paymentId);
}
