package com.example.pg.receipt.domain.enumerate;

/**
 * 영수증 상태.
 * ISSUED: 발급됨 (정상)
 * VOIDED: 무효 (결제 취소 등으로 영수증 무효화)
 */
public enum ReceiptStatus {
    ISSUED,
    VOIDED
}
