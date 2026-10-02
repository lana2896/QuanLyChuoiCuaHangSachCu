package com.oldbook.controller.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.oldbook.dto.auth.*;
import com.oldbook.dto.common.ApiResponse;
import com.oldbook.service.auth.AuthService;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đăng ký tài khoản thành công"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        LoginResponse response = authService.login(request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(
                    value = HttpHeaders.AUTHORIZATION,
                    required = false
            ) String authorization
    ) {

        if (authorization == null
                || !authorization.startsWith("Bearer ")) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(
                            ApiResponse.error(
                                    401,
                                    "Thiếu token xác thực"
                            )
                    );
        }

        String token = authorization.substring(7);

        authService.logout(token);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Đăng xuất thành công"
                )
        );
    }

    @PostMapping("/select-role")
    public ResponseEntity<ApiResponse<LoginResponse>> selectRole(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody SelectRoleRequest request
    ) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(401, "Thiếu token xác thực"));
        }

        String token = authorization.substring(7);

        LoginResponse response = authService.selectRole(token, request.getMaTK());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {

        authService.forgotPassword(request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Đã gửi mã OTP đặt lại mật khẩu"
                )
        );
    }

    @PostMapping("/resend-forgot-password-otp")
    public ResponseEntity<ApiResponse<Void>> resendForgotPasswordOtp(
            @Valid @RequestBody ResendForgotPasswordOtpRequest request
    ) {

        authService.resendForgotPasswordOtp(request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Đã gửi lại mã OTP đặt lại mật khẩu"
                )
        );
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {

        authService.resetPassword(request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Đặt lại mật khẩu thành công"
                )
        );
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request
    ) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        Integer maND = (Integer) authentication.getPrincipal();

        authService.changePassword(maND, request);

        return ResponseEntity.ok(
                ApiResponse.success("Đổi mật khẩu thành công")
        );
    }

    @PostMapping("/verify-register-otp")
    public ResponseEntity<ApiResponse<Void>> verifyRegisterOtp(
            @Valid @RequestBody VerifyRegisterOtpRequest request
    ) {

        authService.verifyRegisterOtp(request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Xác thực OTP thành công, tài khoản đã được kích hoạt"
                )
        );
    }
    @PostMapping("/resend-register-otp")
    public ResponseEntity<ApiResponse<Void>> resendRegisterOtp(
            @Valid @RequestBody ResendRegisterOtpRequest request) {

        authService.resendRegisterOtp(request);

        return ResponseEntity.ok(
                ApiResponse.success("Đã gửi lại mã OTP")
        );
    }
}