package com.example.pg.card_company.util;

import com.example.pg.common.exception.BusinessException;
import com.example.pg.common.exception.ErrorCode;
import com.example.pg.payment.presentation.dto.CardCompanyErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;

/**
 * 카드사 API 4xx/5xx 응답 본문을 파싱해 BusinessException으로 변환한다.
 */
@Component
@RequiredArgsConstructor
public final class CardCompanyErrorParser {

    private final ObjectMapper objectMapper;

    /**
     * HttpStatusCodeException을 카드사 연동 오류 BusinessException으로 변환한다.
     */
    public BusinessException toBusinessException(HttpStatusCodeException e, String operation) {
        String body = e.getResponseBodyAsString();
        if (body != null && !body.isBlank()) {
            try {
                CardCompanyErrorResponse err = objectMapper.readValue(body, CardCompanyErrorResponse.class);
                String msg = err.message() != null ? err.message() : body;
                return new BusinessException(ErrorCode.CARD_COMPANY_API_ERROR, operation + " 실패: " + msg);
            } catch (Exception ignored) {
                return new BusinessException(ErrorCode.CARD_COMPANY_API_ERROR, operation + " 실패: " + body);
            }
        }
        return new BusinessException(ErrorCode.CARD_COMPANY_API_ERROR, operation + " 실패: " + e.getStatusCode());
    }
}
