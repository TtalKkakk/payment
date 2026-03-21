package com.example.pg.payment.command.domain.repository;

import com.example.pg.payment.command.domain.aggregate.Payment;
import com.example.pg.payment.command.domain.vo.PaymentId;

import java.util.Optional;

public interface PaymentCommandRepository {
    void save(Payment payment);
    Optional<Payment> load(PaymentId paymentId);
}
