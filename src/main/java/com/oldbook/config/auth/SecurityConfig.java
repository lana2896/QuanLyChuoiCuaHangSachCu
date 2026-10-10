package com.oldbook.config.auth;

import com.oldbook.dto.common.ApiResponse;
import com.oldbook.filter.auth.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http

                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(authorize -> authorize

                        // HTML shells load JWT from the browser; their API requests remain protected below.
                        .requestMatchers(
                                "/",
                                "/books/**",
                                "/orders",

                                // Trang đơn vị vận chuyển: không đăng nhập, ai có link /shipping/{maCode} đều vào được
                                "/shipping",
                                "/shipping/**",
                                // Trang quản lý đối tác: HTML công khai, dữ liệu lấy qua /api/management/** (có phân quyền)
                                "/management/**",
                                "/cart",
                                "/wishlist",

                                "/admin/accounts",
                                "/store",
                                "/vendor/books",
                                "/store-moderation",

                                "/profile",
                                "/addresses",

                                "/checkout",
                                "/order-success",

                                "/store/orders",

                                "/login",
                                "/register",
                                "/forgot-password",
                                "/change-password",
                                "/verify-register",
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
                                "/api/books/**"
                        ).permitAll()

                        .requestMatchers("/api/admin/**")
                        .hasRole("QUAN_TRI_VIEN")

                        // API đơn vị vận chuyển theo mã: /api/shipping/{maCode}/...
                        // Không dùng tài khoản; service tự giới hạn dữ liệu theo maCode.
                        .requestMatchers("/api/shipping/**")
                        .permitAll()

                        // Quản lý thêm/sửa đối tác vận chuyển
                        .requestMatchers("/api/management/**")
                        .hasAnyRole("QUAN_LY", "QUAN_TRI_VIEN")

                        .requestMatchers("/api/store/moderation/**")
                        .hasAnyRole("QUAN_LY", "QUAN_TRI_VIEN")

                        .requestMatchers("/api/store/**")
                        .hasRole("CHU_CUA_HANG")

                        .requestMatchers("/api/wishlist", "/api/wishlist/**")
                        .hasRole("KHACH_HANG")

                        .anyRequest().authenticated()
                )

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                writeError(
                                        response,
                                        HttpServletResponse.SC_UNAUTHORIZED,
                                        "Phiên đăng nhập không hợp lệ hoặc đã hết hạn. Vui lòng đăng nhập lại."
                                )
                        )
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            String message = accessDeniedException.getMessage();

                            if (message == null || message.isBlank()
                                    || "Access Denied".equalsIgnoreCase(message)) {
                                message = "Bạn không có quyền thực hiện thao tác này.";
                            }

                            writeError(response, HttpServletResponse.SC_FORBIDDEN, message);
                        })
                )

                .formLogin(form -> form.disable())

                .httpBasic(basic -> basic.disable())

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    private static void writeError(HttpServletResponse response, int status, String message)
            throws java.io.IOException {

        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        OBJECT_MAPPER.writeValue(
                response.getWriter(),
                ApiResponse.<Void>error(status, message)
        );
    }
}