package com.example.pg.common.retry.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RetryWorkerProperties.class)
public class RetryWorkerConfig {
}

