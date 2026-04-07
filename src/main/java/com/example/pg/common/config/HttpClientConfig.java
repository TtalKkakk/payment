package com.example.pg.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class HttpClientConfig {

    @Bean
    public RestTemplate restTemplate(ObservationRegistry observationRegistry) {
        RestTemplate restTemplate = new RestTemplate();
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
