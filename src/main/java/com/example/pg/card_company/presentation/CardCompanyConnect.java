package com.example.pg.card_company.presentation;

import com.example.pg.card_company.presentation.dto.BillingKeyTokenResponse;
import com.example.pg.payment.presentation.dto.PaymentApproveResponse;
import com.example.pg.card_company.presentation.dto.RegistrationSessionResponse;

/**
 * 카드사 결제 승인 API 포트.
 * 가이드: POST /api/pg/payments/approve (paymentId, amount, billingKeyToken) → success, approvalNumber, transactionId 등.
 */
public interface CardCompanyConnect {

    /**
     * 카드사에 결제 승인 요청.
     * 동일 paymentId 재요청 시 카드사는 멱등 처리하여 기존 승인 결과를 반환할 수 있음.
     *
     * @param paymentId       PG 결제 ID (멱등·취소 매칭용)
     * @param amount          결제 금액 (0 초과)
     * @param billingKeyToken 빌링키 발급 시 카드사가 발급한 토큰
     * @return 승인 성공 시 approvalNumber·transactionId·approvedAt 포함, 실패 시 resultCode·message
     */
    PaymentApproveResponse approve(String paymentId, long amount, String billingKeyToken);
    RegistrationSessionResponse createRegistrationSession(String returnUrl);
    BillingKeyTokenResponse issueBillingKey(String authCode);

    /**
     * 카드사에 결제 환불(취소) 요청.
     * POST /api/pg/payments/refund Body: { paymentId }. 승인 건만 취소·잔액 복원.
     *
     * @param paymentId PG 결제 ID (승인 시 사용한 값과 동일)
     * @return 환불 성공 시 true, 4xx/실패 시 false
     */
    boolean requestRefund(String paymentId);
}
