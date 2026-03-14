package com.example.pg.receipt.command.application.port;

import com.example.pg.receipt.command.application.port.dto.PaymentSnapshotForReceipt;

import java.util.Optional;

/**
 * 영수증 발급 시 결제 정보를 조회하는 포트 (안티커럽션).
 * receipt 도메인은 payment/merchant 인프라에 직접 의존하지 않고 이 포트만 사용한다.
 */
public interface PaymentPort {

    /**
     * 결제가 존재하는지 여부.
     */
    boolean existsPayment(String paymentId);

    /**
     * 승인 완료(AUTHORIZED)된 결제의 스냅샷을 반환.
     * 없거나 승인 완료가 아니면 empty.
     */
    Optional<PaymentSnapshotForReceipt> findAuthorizedPayment(String paymentId);
}
