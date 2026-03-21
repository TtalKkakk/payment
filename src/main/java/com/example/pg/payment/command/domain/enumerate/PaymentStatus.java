package com.example.pg.payment.command.domain.enumerate;

public enum PaymentStatus {
    READY,          // 결제 생성됨
    AUTHORIZING,    // 승인 진행 중
    AUTHORIZED,     // 승인 성공
    FAILED,         // 승인 실패
    CANCELED        // 취소 완료
}
