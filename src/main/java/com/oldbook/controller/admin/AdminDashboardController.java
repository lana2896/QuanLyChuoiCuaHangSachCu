package com.oldbook.controller.admin;

import com.oldbook.dto.admin.AdminDashboardSummaryResponse;
import com.oldbook.dto.common.ApiResponse;
import com.oldbook.service.admin.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Quyền QUAN_TRI_VIEN được áp cho toàn bộ /api/admin/** trong SecurityConfig.
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/summary")
    public ApiResponse<AdminDashboardSummaryResponse> summary() {

        return ApiResponse.success(
                adminDashboardService.getSummary()
        );
    }
}
