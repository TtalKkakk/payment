package com.example.pg.payment.application;

/**
 * 결제 엔티티에 저장하는 실패 스냅샷 값 정규화(길이·null 처리).
 */
public final class PaymentFailureSnapshotSupport {

    private static final int MAX_MESSAGE_LENGTH = 2000;

    private PaymentFailureSnapshotSupport() {
    }

    public static String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            return "UNKNOWN";
        }
        return code.trim();
    }

    public static String truncateMessage(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        String t = message.trim();
        if (t.length() <= MAX_MESSAGE_LENGTH) {
            return t;
        }
        return t.substring(0, MAX_MESSAGE_LENGTH);
    }

    /** Processor에서 TECHNICAL 스냅샷 메시지용 (예: NPE: null) */
    public static String technicalExceptionSummary(Throwable e) {
        if (e == null) {
            return null;
        }
        String detail = e.getMessage() != null ? e.getMessage() : "";
        return e.getClass().getSimpleName() + ": " + detail;
    }
}
