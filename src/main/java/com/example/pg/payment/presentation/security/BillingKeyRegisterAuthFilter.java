package com.example.pg.payment.presentation.security;

import com.example.pg.common.exception.BusinessException;
import com.example.pg.common.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * /billing-key/register 접근 제어 필터.
 * 브라우저 리다이렉트 플로우이므로 헤더 인증 대신, 쿼리/폼 파라미터 token(HMAC 서명)을 검증한다.
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class BillingKeyRegisterAuthFilter extends OncePerRequestFilter {

    private static final String TOKEN_PARAM = "token";

    private final BillingKeyRegisterTokenVerifier tokenVerifier;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if ("/billing-key/register".equals(uri) && "GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        if ("/billing-key/register/start".equals(uri) && "POST".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        return true;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = request.getParameter(TOKEN_PARAM);
        try {
            BillingKeyRegisterTokenVerifier.Verified verified = tokenVerifier.verifyOrThrow(token);
            request.setAttribute(BillingKeyRegisterTokenVerifier.ATTR_MERCHANT_ID, verified.merchantId());
            request.setAttribute(BillingKeyRegisterTokenVerifier.ATTR_API_KEY, verified.apiKey());
            request.setAttribute(BillingKeyRegisterTokenVerifier.ATTR_RETURN_URL, verified.returnUrl());
            filterChain.doFilter(request, response);
        } catch (BusinessException e) {
            log.warn("[Payment] BillingKey register denied uri={} reason={}", request.getRequestURI(), e.getErrorCode().name());
            response.sendError(HttpServletResponse.SC_FORBIDDEN, ErrorCode.BILLING_KEY_REGISTER_TOKEN_INVALID.getMessageTemplate());
        }
    }
}

