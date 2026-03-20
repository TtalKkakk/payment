package com.example.pg.payment.presentation.security;

import com.example.pg.common.exception.BusinessException;
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
        if ("/card-form/register".equals(uri) && "GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        if ("/card-form/register/start".equals(uri) && "POST".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        return true;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = request.getParameter(TOKEN_PARAM);
        try {
            // GET: 토큰 검증만 수행(페이지 렌더링 단계이므로 nonce 소비 스킵)
            // POST: start 단계에서만 nonce를 소비(재사용 공격 방지)
            boolean consumeNonce = "POST".equalsIgnoreCase(request.getMethod());
            BillingKeyRegisterTokenVerifier.Verified verified = tokenVerifier.verifyOrThrow(token, consumeNonce);
            request.setAttribute(BillingKeyRegisterTokenVerifier.ATTR_MERCHANT_ID, verified.merchantId());
            request.setAttribute(BillingKeyRegisterTokenVerifier.ATTR_API_KEY, verified.apiKey());
            request.setAttribute(BillingKeyRegisterTokenVerifier.ATTR_RETURN_URL, verified.returnUrl());
            filterChain.doFilter(request, response);
        } catch (BusinessException e) {
            log.warn("[Payment] BillingKey register denied uri={} reason={}", request.getRequestURI(), e.getErrorCode().name());
            // 사용자용 에러 페이지로 forward (sendError는 /error로 떨어져 Whitelabel을 유발할 수 있음)
            int statusCode = e.getErrorCode().getStatus().value();
            response.setStatus(statusCode);
            request.setAttribute("statusCode", statusCode);
            request.setAttribute("code", e.getErrorCode().getCode());
            request.setAttribute("message", e.getErrorCode().formatMessage(e.getArgs()));

            request.getRequestDispatcher("/ui-error").forward(request, response);
        }
    }
}

