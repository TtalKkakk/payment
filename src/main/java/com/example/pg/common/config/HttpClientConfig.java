package com.example.pg.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class HttpClientConfig {

    @Bean
    public RestTemplate restTemplate(ObservationRegistry observationRegistry) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);   // 연결 타임아웃 3초
        factory.setReadTimeout(12000);     // 읽기 타임아웃 12초 (카드사 DELAY 10초 + 여유 2초)

        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.setObservationRegistry(observationRegistry);
        return restTemplate;
    }

    /**
     * {@code java.time} 직렬화용 JSR-310 모듈 등록.
     * 웹훅 DTO는 시간 필드를 문자열로 두지만, Redis·기타 JSON 직렬화에 동일 매퍼가 쓰인다.
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }
}
