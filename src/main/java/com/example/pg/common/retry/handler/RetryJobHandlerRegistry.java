package com.example.pg.common.retry.handler;

import com.example.pg.common.retry.handler.RetryJobHandler;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class RetryJobHandlerRegistry {

    private final Map<String, RetryJobHandler> handlerByType;

    public RetryJobHandlerRegistry(List<RetryJobHandler> handlers) {
        this.handlerByType = handlers.stream()
                .collect(Collectors.toUnmodifiableMap(RetryJobHandler::jobType, Function.identity()));
    }

    public RetryJobHandler getRequired(String jobType) {
        RetryJobHandler handler = handlerByType.get(jobType);
        if (handler == null) {
            throw new IllegalStateException("No RetryJobHandler for jobType=" + jobType);
        }
        return handler;
    }
}

