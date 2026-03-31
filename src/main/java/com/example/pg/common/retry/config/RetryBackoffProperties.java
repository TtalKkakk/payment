package com.example.pg.common.retry.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Getter
@ConfigurationProperties(prefix = "app.retry.backoff")
public class RetryBackoffProperties {

    private Policy defaultPolicy = new Policy();
    private Map<String, Policy> byJobType = new HashMap<>();

    public void setDefaultPolicy(Policy defaultPolicy) {
        this.defaultPolicy = defaultPolicy;
    }

    public void setByJobType(Map<String, Policy> byJobType) {
        this.byJobType = byJobType;
    }

}

