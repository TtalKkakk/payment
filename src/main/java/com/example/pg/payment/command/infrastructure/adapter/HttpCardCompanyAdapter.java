package com.example.pg.payment.command.infrastructure.adapter;

import com.example.pg.payment.command.application.port.CardCompanyPort;
import com.example.pg.payment.command.infrastructure.adapter.dto.CardCompanyApproveRequest;
import com.example.pg.payment.command.infrastructure.adapter.dto.CardCompanyApproveResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 카드사 결제 승인 API HTTP 어댑터.
 * 가이드: POST /api/pg/payments/approve (paymentId, amount, billingKeyToken) → success, approvalNumber, transactionId 등.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HttpCardCompanyAdapter implements CardCompanyPort {

    private static final String APPROVE_PATH = "api/pg/payments/approve";

    private final RestTemplate restTemplate;

    @Value("${app.card-company.default-base-url:http://localhost:8081}")
    private String baseUrl;

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
