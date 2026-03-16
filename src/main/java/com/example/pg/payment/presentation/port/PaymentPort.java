package com.example.pg.payment.presentation.port;

import com.example.pg.payment.command.application.adapter.dto.PaymentSnapshotForReceiptDto;

import java.util.Optional;

/**
 * 영수증 발급 시 결제 정보를 조회하는 포트.
 * receipt 모듈이 이 포트만 사용해 결제 데이터에 접근한다.
 */
public interface PaymentPort {

    boolean existsPayment(String paymentId);

    /**
     * 승인 완료(AUTHORIZED)된 결제 스냅샷을 반환. 없거나 승인 완료가 아니면 empty.
     */
    Optional<PaymentSnapshotForReceiptDto> findAuthorizedPayment(String paymentId);
}
