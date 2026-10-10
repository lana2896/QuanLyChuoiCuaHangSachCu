package com.oldbook.controller.identity;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.oldbook.dto.common.ApiResponse;
import com.oldbook.dto.identity.AdminAccountResponse;
import com.oldbook.dto.identity.ChangeRoleRequest;
import com.oldbook.dto.identity.CreateAccountRequest;
import com.oldbook.dto.identity.LockAccountRequest;
import com.oldbook.filter.auth.JwtAuthenticationFilter.AuthenticatedUserDetails;
import com.oldbook.service.identity.AdminAccountService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/accounts")
@RequiredArgsConstructor
public class AdminAccountController {

    private final AdminAccountService adminAccountService;

    @GetMapping
    public ApiResponse<List<AdminAccountResponse>> getAll(
            @RequestParam(required = false) String tuKhoa,
            @RequestParam(required = false) String vaiTro,
            @RequestParam(required = false) String trangThai
    ) {
        return ApiResponse.success(
                adminAccountService.getAll(
                        tuKhoa,
                        vaiTro,
                        trangThai
                )
        );
    }

    @PostMapping
    public ApiResponse<AdminAccountResponse> create(
            @Valid @RequestBody CreateAccountRequest request
    ) {
        AuthenticatedUserDetails currentUser =
                getCurrentUser();

        AdminAccountResponse response =
                adminAccountService.create(
                        request,
                        currentUser.maTK(),
                        getClientIp()
                );

        return ApiResponse.success(response);
    }

    @PutMapping("/{maTK}/lock")
    public ApiResponse<Void> lock(
            @PathVariable Integer maTK,
            @Valid @RequestBody LockAccountRequest request
    ) {
        AuthenticatedUserDetails currentUser =
                getCurrentUser();

        adminAccountService.lock(
                maTK,
                request,
                currentUser.maTK(),
                getClientIp()
        );

        return ApiResponse.success(
                "Khóa tài khoản thành công"
        );
    }

    @PutMapping("/{maTK}/unlock")
    public ApiResponse<Void> unlock(
            @PathVariable Integer maTK
    ) {
        AuthenticatedUserDetails currentUser =
                getCurrentUser();

        adminAccountService.unlock(
                maTK,
                currentUser.maTK(),
                getClientIp()
        );

        return ApiResponse.success(
                "Mở khóa tài khoản thành công"
        );
    }

    @PutMapping("/{maTK}/role")
    public ApiResponse<AdminAccountResponse> changeRole(
            @PathVariable Integer maTK,
            @Valid @RequestBody ChangeRoleRequest request
    ) {
        AuthenticatedUserDetails currentUser =
                getCurrentUser();

        AdminAccountResponse response =
                adminAccountService.changeRole(
                        maTK,
                        request,
                        currentUser.maTK(),
                        getClientIp()
                );

        return ApiResponse.success(response);
    }

    private AuthenticatedUserDetails getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null) {
            throw new IllegalStateException(
                    "Không xác định được người dùng hiện tại"
            );
        }

        Object details =
                authentication.getDetails();

        if (!(details instanceof AuthenticatedUserDetails userDetails)) {
            throw new IllegalStateException(
                    "Thông tin xác thực không hợp lệ"
            );
        }

        return userDetails;
    }

    private String getClientIp() {

        HttpServletRequest request =
                getCurrentHttpRequest();

        String xForwardedFor =
                request.getHeader("X-Forwarded-For");

        if (xForwardedFor != null
                && !xForwardedFor.isBlank()) {

            return xForwardedFor
                    .split(",")[0]
                    .trim();
        }

        String xRealIp =
                request.getHeader("X-Real-IP");

        if (xRealIp != null
                && !xRealIp.isBlank()) {

            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    private HttpServletRequest getCurrentHttpRequest() {

        return ((org.springframework.web.context.request.ServletRequestAttributes)
                org.springframework.web.context.request.RequestContextHolder
                        .currentRequestAttributes())
                .getRequest();
    }
}
