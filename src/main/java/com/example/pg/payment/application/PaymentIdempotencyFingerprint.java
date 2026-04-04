package com.example.pg.payment.application;

import com.example.pg.payment.presentation.dto.CreatePaymentRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 결제 생성 멱등 판별용 요청 지문(SHA-256 hex). 필드 순서·구분자 고정.
 */
public final class PaymentIdempotencyFingerprint {

    private PaymentIdempotencyFingerprint() {
    }

    public static String sha256Hex(CreatePaymentRequest request) {
        String canonical = String.join(
                "\u001e",
                Long.toString(request.amount()),
                request.merchantOrderId(),
                request.orderName(),
                request.customerEmail() == null ? "" : request.customerEmail(),
                request.customerName(),
                request.callbackUrl(),
                request.billingKey(),
                request.cardCompanyCode()
        );
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
