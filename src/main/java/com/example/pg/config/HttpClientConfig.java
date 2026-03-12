package com.example.pg.config;

import com.example.pg.payment.command.application.port.CardCompanyPort;
import com.example.pg.payment.presentation.adapter.HttpCardCompanyAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class HttpClientConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public CardCompanyPort cardCompanyPort(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${app.card-company.default-base-url:http://localhost:8080/}") String defaultBaseUrl) {
        return new HttpCardCompanyAdapter(restTemplate, objectMapper, defaultBaseUrl);
    }
}
