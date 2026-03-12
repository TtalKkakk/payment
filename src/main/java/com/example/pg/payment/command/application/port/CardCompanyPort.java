package com.example.pg.payment.command.application.port;

import java.time.LocalDateTime;

/**
 * 카드사 결제 승인 API 포트.
 * 가이드: POST /api/pg/payments/approve (paymentId, amount, billingKeyToken) → success, approvalNumber, transactionId 등.
 */
public interface CardCompanyPort {

    /**
     * 카드사에 결제 승인 요청.
     * 동일 paymentId 재요청 시 카드사는 멱등 처리하여 기존 승인 결과를 반환할 수 있음.
     *
     * @param paymentId       PG 결제 ID (멱등·취소 매칭용)
     * @param amount          결제 금액 (0 초과)
     * @param billingKeyToken 빌링키 발급 시 카드사가 발급한 토큰
     * @return 승인 성공 시 approvalNumber·transactionId·approvedAt 포함, 실패 시 resultCode·message
     */
    ApproveResult approve(String paymentId, long amount, String billingKeyToken);

    record ApproveResult(
            boolean success,
            String paymentId,
            String approvalNumber,
            String transactionId,
            LocalDateTime approvedAt,
            String resultCode,
            String message
    ) {
        public static ApproveResult success(String paymentId, String approvalNumber, String transactionId,
                                            LocalDateTime approvedAt) {
            return new ApproveResult(true, paymentId, approvalNumber, transactionId, approvedAt, "0000", null);
        }

        public static ApproveResult failure(String paymentId, String resultCode, String message) {
            return new ApproveResult(false, paymentId, null, null, null, resultCode, message);
        }
    }
}
