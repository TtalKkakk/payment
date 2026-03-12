package com.example.pg.payment.presentation.adapter;

import com.example.pg.payment.presentation.dto.BillingKeyRequest;
import com.example.pg.payment.presentation.dto.BillingKeyResponse;
import com.example.pg.payment.presentation.dto.SessionRequest;
import com.example.pg.payment.presentation.dto.SessionResponse;
import com.example.pg.exception.BusinessException;
import com.example.pg.exception.ErrorCode;
import com.example.pg.payment.command.application.port.CardCompanyPort;
import com.example.pg.payment.command.application.port.dto.BillingKeyTokenDto;
import com.example.pg.payment.command.application.port.dto.RegistrationSessionDto;
import com.example.pg.payment.presentation.dto.CardCompanyApproveRequest;
import com.example.pg.payment.presentation.dto.CardCompanyApproveResponse;
import com.example.pg.payment.presentation.dto.CardCompanyErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 카드사 결제 승인 API HTTP 어댑터.
 * 가이드: POST /api/pg/payments/approve (paymentId, amount, billingKeyToken) → success, approvalNumber, transactionId 등.
 */
@Slf4j
@RequiredArgsConstructor
public class HttpCardCompanyAdapter implements CardCompanyPort {

    private static final String APPROVE_PATH = "api/pg/payments/approve";
    private static final String SESSION_PATH = "api/card-registration-session";
    private static final String BILLING_KEY_PATH = "api/billing-keys";
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    @Override
    public ApproveResult approve(String paymentId, long amount, String billingKeyToken) {
        String url = baseUrl.endsWith("/") ? baseUrl + APPROVE_PATH : baseUrl + "/" + APPROVE_PATH;
        CardCompanyApproveRequest request = new CardCompanyApproveRequest(paymentId, amount, billingKeyToken);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CardCompanyApproveRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<CardCompanyApproveResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    CardCompanyApproveResponse.class
            );
            CardCompanyApproveResponse body = response.getBody();
            if (body == null) {
                return ApproveResult.failure(paymentId, "E999", "카드사 응답이 비어 있습니다.");
            }
            return toApproveResult(body);
        } catch (Exception e) {
            log.warn("카드사 결제 승인 요청 실패 paymentId={}, url={}", paymentId, url, e);
            return ApproveResult.failure(paymentId, "E999", e.getMessage());
        }
    }

    @Override
    public RegistrationSessionDto createRegistrationSession(String returnUrl) {
        String url = baseUrl.endsWith("/") ? baseUrl + SESSION_PATH : baseUrl + "/" + SESSION_PATH;
        SessionRequest request = new SessionRequest(returnUrl);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<SessionRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<SessionResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    SessionResponse.class
            );
            SessionResponse body = response.getBody();
            if (body == null) {
                throw new BusinessException(ErrorCode.CARD_COMPANY_API_ERROR, "카드사 등록 세션 응답이 비어 있습니다.");
            }
            return new RegistrationSessionDto(body.token(), body.registrationUrl());
        } catch (HttpStatusCodeException e) {
            throw toBusinessException(e, "등록 세션 생성");
        }
    }

    @Override
    public BillingKeyTokenDto issueBillingKey(String authCode) {
        String url = baseUrl.endsWith("/") ? baseUrl + BILLING_KEY_PATH : baseUrl + "/" + BILLING_KEY_PATH;
        BillingKeyRequest request = new BillingKeyRequest(authCode);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<BillingKeyRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<BillingKeyResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    BillingKeyResponse.class
            );
            BillingKeyResponse body = response.getBody();
            if (body == null) {
                throw new BusinessException(ErrorCode.CARD_COMPANY_API_ERROR, "카드사 빌링키 응답이 비어 있습니다.");
            }
            return new BillingKeyTokenDto(body.billingKeyToken());
        } catch (HttpStatusCodeException e) {
            throw toBusinessException(e, "빌링키 발급");
        }
    }

    private BusinessException toBusinessException(HttpStatusCodeException e, String operation) {
        String body = e.getResponseBodyAsString();
        if (body != null && !body.isBlank()) {
            try {
                CardCompanyErrorResponse err = objectMapper.readValue(body, CardCompanyErrorResponse.class);
                String msg = err.message() != null ? err.message() : body;
                return new BusinessException(ErrorCode.CARD_COMPANY_API_ERROR, operation + " 실패: " + msg);
            } catch (Exception ignored) {
                // JSON 파싱 실패 시 본문 그대로 사용
            }
        }
        return new BusinessException(ErrorCode.CARD_COMPANY_API_ERROR, operation + " 실패: " + e.getStatusCode());
    }

    private ApproveResult toApproveResult(CardCompanyApproveResponse body) {
        LocalDateTime approvedAt = parseApprovedAt(body.approvedAt());
        if (body.success()) {
            return ApproveResult.success(
                    body.paymentId(),
                    body.approvalNumber(),
                    body.transactionId(),
                    approvedAt != null ? approvedAt : LocalDateTime.now()
            );
        }
        return ApproveResult.failure(
                body.paymentId(),
                body.resultCode() != null ? body.resultCode() : "E999",
                body.message()
        );
    }

    private LocalDateTime parseApprovedAt(String approvedAt) {
        if (approvedAt == null || approvedAt.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(approvedAt, DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException e) {
            try {
                return LocalDateTime.parse(approvedAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (DateTimeParseException e2) {
                log.debug("approvedAt 파싱 실패: {}", approvedAt);
                return null;
            }
        }
    }
}
