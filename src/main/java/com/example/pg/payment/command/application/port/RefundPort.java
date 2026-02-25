package com.example.pg.payment.command.application.port;

/**
 * 카드사/은행에 환불 요청을 보내는 아웃바운드 포트.
 * 결제 취소 시 AUTHORIZED 상태의 결제에 대해 환불을 요청한다.
 */
public interface RefundPort {

    /**
     * 카드사/은행에 환불 요청.
     *
     * @param paymentId 결제 ID
     * @param amount    환불 금액
     * @return 환불 성공 여부
     */
    boolean requestRefund(String paymentId, long amount);
}
