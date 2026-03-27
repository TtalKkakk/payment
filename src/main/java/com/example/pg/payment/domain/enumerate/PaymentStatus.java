package com.example.pg.payment.domain.enumerate;

public enum PaymentStatus {
    READY,          // 결제 생성됨
    AUTHORIZING,    // 승인 진행 중
    AUTHORIZED,     // 승인 성공
    AUTHORIZE_FAILED,         // 승인 실패
    CANCELED,       // 취소 완료
    ABORTED         // 결제 생성 후 승인 시작 실패로 무효화(보상)
}
