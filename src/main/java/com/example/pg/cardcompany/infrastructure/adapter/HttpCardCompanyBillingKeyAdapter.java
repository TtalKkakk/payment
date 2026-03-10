package com.example.pg.cardcompany.infrastructure.adapter;

import com.example.pg.cardcompany.domain.port.CardCompanyBillingKeyPort;
import com.example.pg.cardcompany.domain.result.BillingKeyTokenResult;
import com.example.pg.cardcompany.domain.result.RegistrationSessionResult;
import com.example.pg.cardcompany.infrastructure.adapter.dto.BillingKeyRequest;
import com.example.pg.cardcompany.infrastructure.adapter.dto.BillingKeyResponse;
import com.example.pg.cardcompany.infrastructure.adapter.dto.CardCompanyErrorResponse;
import com.example.pg.cardcompany.infrastructure.adapter.dto.SessionRequest;
import com.example.pg.cardcompany.infrastructure.adapter.dto.SessionResponse;
import com.example.pg.exception.BusinessException;
import com.example.pg.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

/**
 * 카드사 빌링키 API HTTP 어댑터.
 * 가이드라인: POST /card-registration-session, POST /billing-keys.
 */
public class HttpCardCompanyBillingKeyAdapter implements CardCompanyBillingKeyPort {

    private final String baseUrl;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public HttpCardCompanyBillingKeyAdapter(String baseUrl, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public RegistrationSessionResult createRegistrationSession(String returnUrl) {
        String url = baseUrl + "card-registration-session";
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
                throw new BusinessException(ErrorCode.INTERNAL, "카드사 등록 세션 응답이 비어 있습니다.");
            }
            return new RegistrationSessionResult(body.token(), body.registrationUrl());
        } catch (HttpStatusCodeException e) {
            throw toBusinessException(e, "등록 세션 생성");
        }
    }

    @Override
    public BillingKeyTokenResult issueBillingKey(String authCode) {
        String url = baseUrl + "billing-keys";
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
                throw new BusinessException(ErrorCode.INTERNAL, "카드사 빌링키 응답이 비어 있습니다.");
            }
            return new BillingKeyTokenResult(body.billingKeyToken());
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
                return new BusinessException(ErrorCode.INTERNAL, operation + " 실패: " + msg);
            } catch (Exception ignored) {
                // fallback
            }
        }
        return new BusinessException(ErrorCode.INTERNAL, operation + " 실패: " + e.getStatusCode());
    }
}
