package com.example.pg.payment.command.infrastructure.persistence;

import com.example.pg.payment.command.domain.aggregate.Payment;
import com.example.pg.payment.command.domain.repository.PaymentCommandRepository;
import com.example.pg.payment.command.domain.vo.PaymentId;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaPaymentCommandRepository implements PaymentCommandRepository {
    private final EntityManager em;
    @Override
    public void save(Payment payment) {
        em.persist(payment);
    }

    @Override
    public Optional<Payment> load(PaymentId paymentId) {
        return Optional.ofNullable(em.find(Payment.class, paymentId.getValue()));
    }
}
