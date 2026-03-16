package com.example.pg.payment.presentation.impl;

import com.example.pg.common.exception.BusinessException;
import com.example.pg.common.exception.ErrorCode;
import com.example.pg.common.util.HttpOutbound;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;

/**
 * 카드사 API POST 호출을 공통 처리한다.
 * 4xx/5xx는 BusinessException으로, 네트워크/기타 오류도 BusinessException으로 변환해 호출부 try-catch를 최소화한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CardCompanyApiTemplate {

    private final HttpOutbound httpOutbound;
    private final CardCompanyErrorParser errorParser;

    /**
     * POST 후 응답 본문을 역직렬화해 반환한다.
     * 4xx/5xx 시 errorParser로 BusinessException throw, 본문 null 시 BusinessException, 그 외 예외도 BusinessException으로 래핑해 throw.
     *
     * @param operation 로그/에러 메시지용 작업명 (예: "등록 세션 생성", "빌링키 발급")
     * @return 응답 본문 (null이면 BusinessException throw)
     */
    public <T> T postForObject(String url, Object requestBody, Class<T> responseType, String operation) {
        try {
            T body = httpOutbound.postForObject(url, requestBody, responseType);
            if (body == null) {
                throw new BusinessException(ErrorCode.CARD_COMPANY_API_ERROR, "카드사 응답이 비어 있습니다.");
            }
            return body;
        } catch (HttpStatusCodeException e) {
            throw errorParser.toBusinessException(e, operation);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[Payment] CardCompany API call failed url={} operation={}", url, operation, e);
            throw new BusinessException(ErrorCode.CARD_COMPANY_API_ERROR, operation + " 실패: " + e.getMessage());
        }
    }
}
