package com.example.pg.common.config.filter;

import com.example.pg.merchant.query.application.MerchantQueryService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 가맹점 API 요청에 X-API-KEY, X-API-SECRET 인증 적용.
 * 아래 리스트에 해당하는 경로만 필터를 적용하지 않고, 나머지는 전부 인증 필요.
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class MerchantAuthFilter extends OncePerRequestFilter {

    public static final String MERCHANT_ID_ATTRIBUTE = "merchantId";

    private static final String API_KEY_HEADER = "X-API-KEY";
    private static final String API_SECRET_HEADER = "X-API-SECRET";

    /**
     * 필터 미적용 경로 접두사.
     * 이 목록에 해당하는 요청에는 인증 검사 없이 통과시킨다.
     * 새로 인증 없이 열 경로는 이 리스트에만 추가하면 된다.
     */
    private static final List<String> FILTER_EXCLUDED_PATH_PREFIXES = List.of(
            "/api/admin/",
            "/admin/",
            "/actuator/",
            "/error",
            "/swagger-ui",
            "/v3/api-docs"
    );

    private final MerchantQueryService merchantQueryService;

    /** 가맹점 API는 /api/ 하위만 적용. 그 외 경로는 통과시켜 404 등 정상 처리. */
    private static final String MERCHANT_API_PATH_PREFIX = "/api/";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!request.getRequestURI().startsWith(MERCHANT_API_PATH_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }
        if (isFilterExcluded(request.getRequestURI(), request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String apiKey = request.getHeader(API_KEY_HEADER);
        String apiSecret = request.getHeader(API_SECRET_HEADER);

        var merchantId = merchantQueryService.authenticate(apiKey, apiSecret);

        if (merchantId.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"Invalid or missing API credentials\"}");
            return;
        }

        request.setAttribute(MERCHANT_ID_ATTRIBUTE, merchantId.get());
        filterChain.doFilter(request, response);
    }

    /**
     * 이 경로/메서드에는 필터를 적용하지 않는다 (인증 생략).
     */
    private boolean isFilterExcluded(String requestUri, String method) {
        // 가맹점 신청·심사 조회·삭제·API 키 조회 - 사업자번호+비밀번호로 하므로 API 키 불필요
        if ("POST".equalsIgnoreCase(method) && "/api/merchant-applications".equals(requestUri)) {
            return true;
        }
        if ("POST".equalsIgnoreCase(method) && (requestUri.endsWith("/merchant-applications/business-number")
                || requestUri.endsWith("/merchant-applications/delete"))) {
            return true;
        }
        // API 키 조회는 Merchant 쪽에서 제공 (사업자번호+비밀번호만 필요)
        if ("POST".equalsIgnoreCase(method) && requestUri.endsWith("/merchants/credentials")) {
            return true;
        }
        for (String prefix : FILTER_EXCLUDED_PATH_PREFIXES) {
            if (requestUri.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
