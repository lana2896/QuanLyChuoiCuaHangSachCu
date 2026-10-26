package com.oldbook.controller.admin;

import com.oldbook.dto.admin.AdminRevenueReportResponse;
import com.oldbook.dto.common.ApiResponse;
import com.oldbook.dto.management.StatsDto.DanhMucThongKe;
import com.oldbook.dto.management.StatsDto.DongTienSan;
import com.oldbook.service.admin.AdminReportService;
import com.oldbook.service.management.OperationsStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

// Quyền QUAN_TRI_VIEN được áp cho toàn bộ /api/admin/** trong SecurityConfig.
@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final AdminReportService adminReportService;
    private final OperationsStatsService operationsStatsService;

    // Dòng tiền toàn chuỗi và doanh thu theo danh mục: chỉ xem và xuất CSV, dùng chung số liệu với trang Thống kê của Quản lý.
    @GetMapping("/dong-tien")
    public ApiResponse<DongTienSan> dongTien(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay) {
        return ApiResponse.success(operationsStatsService.dongTien(tuNgay, denNgay));
    }

    @GetMapping("/theo-danh-muc")
    public ApiResponse<List<DanhMucThongKe>> theoDanhMuc(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay) {
        return ApiResponse.success(operationsStatsService.danhMuc(tuNgay, denNgay));
    }

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
