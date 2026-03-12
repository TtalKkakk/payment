package com.example.pg.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * REST API 엔드포인트(@RestController)에만 /api prefix 적용.
 * HTML을 반환하는 @Controller는 대상에서 제외한다.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/api", type -> type.isAnnotationPresent(RestController.class));
    }
}
