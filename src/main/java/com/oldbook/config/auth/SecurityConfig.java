package com.oldbook.config.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.oldbook.filter.auth.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // REST API dùng JWT nên không sử dụng CSRF của form/session
                .csrf(csrf -> csrf.disable())

                // Không dùng session để lưu đăng nhập
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Phân quyền endpoint
                .authorizeHttpRequests(authorize -> authorize

                        // MỞ KHÓA CHO GIAO DIỆN WEB
                        .requestMatchers(
                                "/",
                                "/books/**",
                                "/orders",
                                "/cart",
                                "/wishlist",
                                "/login",
                                "/register",
                                "/css/**",
                                "/js/**",
                                "/images/**"
                        ).permitAll()

                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/select-role",
                                "/api/auth/verify-register-otp",
                                "/api/auth/resend-register-otp",
                                "/api/auth/forgot-password",
                                "/api/auth/resend-forgot-password-otp",
                                "/api/auth/reset-password"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/sach/**"
                        ).permitAll()

                        .requestMatchers("/api/admin/**")
                        .hasRole("QUAN_TRI_VIEN")

                        .anyRequest().authenticated()
                )

                // Không sử dụng form login mặc định
                .formLogin(form -> form.disable())

                // Không sử dụng HTTP Basic
                .httpBasic(basic -> basic.disable())

                // Đặt JWT filter trước UsernamePasswordAuthenticationFilter
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}