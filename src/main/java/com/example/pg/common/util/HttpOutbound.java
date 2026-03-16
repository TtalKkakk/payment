package com.example.pg.common.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 공통 아웃바운드 HTTP 전송 구현체.
 * RestTemplate으로 JSON POST 요청을 보내며, 카드사 어댑터·웹훅 어댑터에서 공유한다.
 * <p>
 * 예외는 catch하지 않고 그대로 전파한다.
 * 4xx/5xx 시 {@link org.springframework.web.client.HttpStatusCodeException},
 * 네트워크/직렬화 오류 시 {@link org.springframework.web.client.RestClientException} 등이 발생한다.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HttpOutbound {

    private final RestTemplate restTemplate;

    public void post(String url, String bodyJson, Map<String, String> extraHeaders) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (extraHeaders != null) {
            extraHeaders.forEach(headers::set);
        }
        HttpEntity<String> entity = new HttpEntity<>(bodyJson, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        log.debug("HTTP POST 완료 url={}, status={}", url, response.getStatusCode());
    }

    public <T> T postForObject(String url, Object requestBody, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.POST, entity, responseType);
        return response.getBody();
    }
}
