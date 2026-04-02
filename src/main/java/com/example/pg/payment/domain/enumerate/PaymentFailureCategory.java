package com.example.pg.payment.domain.enumerate;

/**
 * 결제 마지막 실패 원인 분류(가맹점 안내·모니터링용).
 * <ul>
 *   <li>{@link #BUSINESS} — 카드사가 정상 응답으로 거절/실패를 반환한 경우</li>
 *   <li>{@link #TECHNICAL} — PG~카드사 통신 예외, 타임아웃 등 기술적 실패</li>
 * </ul>
 */
public enum PaymentFailureCategory {
    BUSINESS,
    TECHNICAL
}
