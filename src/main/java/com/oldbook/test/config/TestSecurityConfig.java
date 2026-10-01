package com.oldbook.test.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Mở các URL test (không cần đăng nhập) để thử nhanh.
 * Chỉ áp dụng cho /api/test/** và /api/test, KHÔNG ảnh hưởng phần còn lại của web.
 * Xoá file này khi không cần test nữa.
 */
@Configuration
public class TestSecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/test", "/api/test/**")
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
