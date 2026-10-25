package com.oldbook.controller.admin;

import com.oldbook.dto.admin.AdminUserProfileResponse;
import com.oldbook.dto.common.ApiResponse;
import com.oldbook.service.admin.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping("/{maND}")
    public ApiResponse<AdminUserProfileResponse> getProfile(
            @PathVariable Integer maND
    ) {
        return ApiResponse.success(
                adminUserService.getProfile(maND)
        );
    }
}
