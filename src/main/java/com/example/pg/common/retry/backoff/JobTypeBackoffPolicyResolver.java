package com.example.pg.common.retry.backoff;

import com.example.pg.common.retry.backoff.impl.ExponentialJitterBackoffPolicy;
import com.example.pg.common.retry.config.RetryBackoffProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JobTypeBackoffPolicyResolver {

    private final RetryBackoffProperties props;
    private final Map<String, BackoffPolicy> cache = new ConcurrentHashMap<>();

    public JobTypeBackoffPolicyResolver(RetryBackoffProperties props) {
        this.props = props;
    }

    public BackoffPolicy resolve(String jobType) {
        return cache.computeIfAbsent(jobType == null ? "" : jobType, jt -> toPolicy(jt));
    }

    private BackoffPolicy toPolicy(String jobType) {
        RetryBackoffProperties.Policy configured = props.getByJobType().get(jobType);
        RetryBackoffProperties.Policy p = configured != null ? configured : props.getDefaultPolicy();
        return new ExponentialJitterBackoffPolicy(
                Duration.ofMillis(p.getInitialDelayMs()),
                p.getMultiplier(),
                Duration.ofMillis(p.getMaxDelayMs()),
                p.getJitterRatio()
        );
    }
}

