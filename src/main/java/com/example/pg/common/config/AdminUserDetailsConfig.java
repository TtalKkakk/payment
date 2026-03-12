package com.example.pg.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/**
 * 관리자 로그인용 계정.
 * app.admin.username, app.admin.password 로 설정 (application.yml).
 */
@Configuration
public class AdminUserDetailsConfig {

    @Bean
    public UserDetailsService adminUserDetailsService(
            @Value("${app.admin.username}") String username,
            @Value("${app.admin.password}") String rawPassword,
            PasswordEncoder passwordEncoder
    ) {
        var admin = User.builder()
                .username(username)
                .password(passwordEncoder.encode(rawPassword))
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(admin);
    }
}
