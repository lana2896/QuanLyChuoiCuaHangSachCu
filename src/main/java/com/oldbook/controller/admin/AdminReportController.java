package com.oldbook.controller.admin;

import com.oldbook.dto.admin.AdminRevenueReportResponse;
import com.oldbook.dto.common.ApiResponse;
import com.oldbook.service.admin.AdminReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

// Quyền QUAN_TRI_VIEN được áp cho toàn bộ /api/admin/** trong SecurityConfig.
@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final AdminReportService adminReportService;

    // Ngày dạng yyyy-MM-dd; cả hai ngày đều tính vào kỳ.
    @GetMapping("/revenue")
    public ApiResponse<AdminRevenueReportResponse> revenue(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate tuNgay,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate denNgay
    ) {

        return ApiResponse.success(
                adminReportService.getRevenue(tuNgay, denNgay)
        );
    }
}
