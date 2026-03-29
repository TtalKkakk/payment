package com.example.pg.card_company.presentation.impl;

import com.example.pg.common.util.DateTimeParseUtil;
import com.example.pg.payment.presentation.dto.PaymentApproveResponse;
import com.example.pg.card_company.presentation.dto.BillingKeyRequest;
import com.example.pg.card_company.presentation.dto.BillingKeyResponse;
import com.example.pg.card_company.presentation.dto.SessionRequest;
import com.example.pg.card_company.presentation.dto.SessionResponse;
import com.example.pg.card_company.presentation.CardCompanyConnect;
import com.example.pg.card_company.presentation.dto.BillingKeyTokenResponse;
import com.example.pg.card_company.presentation.dto.RegistrationSessionResponse;
import com.example.pg.card_company.presentation.dto.CardCompanyApproveRequest;
import com.example.pg.card_company.presentation.dto.CardCompanyApproveResponse;
import com.example.pg.payment.presentation.dto.RefundRequest;
import com.example.pg.payment.presentation.dto.RefundResponse;
import com.example.pg.card_company.util.CardCompanyApiTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

/**
 * 카드사 결제 승인 API HTTP 어댑터.
 * 공통 CardCompanyApiTemplate으로 호출·예외 변환을 처리하고, 성공 시 응답 변환만 담당한다.
 */
@Slf4j
@RequiredArgsConstructor
public class CardCompanyAConnectImpl implements CardCompanyConnect {

    private static final String APPROVE_PATH = "api/pg/payments/approve";
    private static final String REFUND_PATH = "api/pg/payments/refund";
    private static final String SESSION_PATH = "api/card-registration-session";
    private static final String BILLING_KEY_PATH = "api/billing-keys";

    private final CardCompanyApiTemplate apiTemplate;
    private final String baseUrl;

    @Override
    public PaymentApproveResponse approve(String paymentId, long amount, String billingKeyToken) {
        String url = buildUrl(APPROVE_PATH);
        CardCompanyApproveRequest request = new CardCompanyApproveRequest(paymentId, amount, billingKeyToken);
        try {
            CardCompanyApproveResponse body = apiTemplate.postForObject(
                    url, request, CardCompanyApproveResponse.class, "결제 승인");
            return toApproveResult(body);
        } catch (Exception e) {
            log.warn("[CardCompany] CardCompany approve failed paymentId={} url={}", paymentId, url, e);
            return PaymentApproveResponse.failure(paymentId, "E999", e.getMessage());
        }
    }

    @Override
    public RegistrationSessionResponse createRegistrationSession(String returnUrl) {
        log.debug("[CardCompany] request to card company for registration form");
        String url = buildUrl(SESSION_PATH);
        SessionRequest request = new SessionRequest(returnUrl);
        SessionResponse body = apiTemplate.postForObject(url, request, SessionResponse.class, "등록 페이지 생성");
        return new RegistrationSessionResponse(body.token(), body.registrationUrl());
    }

    @Override
    public BillingKeyTokenResponse issueBillingKey(String authCode) {
        log.debug("[CardCompany] request to card company for billing key");
        String url = buildUrl(BILLING_KEY_PATH);
        BillingKeyRequest request = new BillingKeyRequest(authCode);
        BillingKeyResponse body = apiTemplate.postForObject(url, request, BillingKeyResponse.class, "빌링키 발급");
        return new BillingKeyTokenResponse(
                body.billingKeyToken(),
                body.cardBrand(),
                body.cardNumberMasked(),
                body.expiryMasked()
        );
    }

    @Override
    public boolean requestRefund(String paymentId) {
        String url = buildUrl(REFUND_PATH);
        RefundRequest request = new RefundRequest(paymentId);
        try {
            RefundResponse body = apiTemplate.postForObject(
                    url, request, RefundResponse.class, "환불");
            return body != null && body.success();
        } catch (Exception e) {
            log.warn("[CardCompany] CardCompany refund failed paymentId={} url={}", paymentId, url, e);
            return false;
        }
    }

    private String buildUrl(String path) {
        return baseUrl.endsWith("/") ? baseUrl + path : baseUrl + "/" + path;
    }

    private PaymentApproveResponse toApproveResult(CardCompanyApproveResponse body) {
        LocalDateTime approvedAt = DateTimeParseUtil.parseIsoOrNull(body.approvedAt());
        if (body.success()) {
            return PaymentApproveResponse.success(
                    body.paymentId(),
                    body.approvalNumber(),
                    body.transactionId(),
                    approvedAt != null ? approvedAt : LocalDateTime.now()
            );
        }
        return PaymentApproveResponse.failure(
                body.paymentId(),
                body.resultCode() != null ? body.resultCode() : "E999",
                body.message()
        );
    }
}
