package com.oldbook.controller.admin;

import com.oldbook.dto.admin.AdminAuditLogResponse;
import com.oldbook.dto.admin.AdminAuditLogSummaryResponse;
import com.oldbook.dto.catalog.PageResponse;
import com.oldbook.dto.common.ApiResponse;
import com.oldbook.service.admin.AdminAuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/audit-logs")
@RequiredArgsConstructor
public class AdminAuditLogController {

    private final AdminAuditLogService adminAuditLogService;


    /**
     * =========================================================
     * Danh sách nhật ký
     * =========================================================
     */
    @GetMapping
    public ApiResponse<PageResponse<AdminAuditLogResponse>> search(
            @RequestParam(required = false)
            String tuKhoa,

            @RequestParam(required = false)
            String hanhDong,

            @RequestParam(required = false)
            String vaiTro,

            @RequestParam(required = false)
            String diaChiIp,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate tuNgay,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate denNgay,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size
    ) {

        return ApiResponse.success(
                adminAuditLogService.search(
                        tuKhoa,
                        hanhDong,
                        vaiTro,
                        diaChiIp,
                        tuNgay,
                        denNgay,
                        page,
                        size
                )
        );
    }


    /**
     * =========================================================
     * KPI
     * =========================================================
     */
    @GetMapping("/summary")
    public ApiResponse<AdminAuditLogSummaryResponse> summary() {

        return ApiResponse.success(
                adminAuditLogService.getSummary()
        );
    }


    /**
     * =========================================================
     * Xuất CSV
     * =========================================================
     */
    @GetMapping("/export")
    public ApiResponse<List<AdminAuditLogResponse>> export(
            @RequestParam(required = false)
            String tuKhoa,

            @RequestParam(required = false)
            String hanhDong,

            @RequestParam(required = false)
            String vaiTro,

            @RequestParam(required = false)
            String diaChiIp,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate tuNgay,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate denNgay
    ) {

        return ApiResponse.success(
                adminAuditLogService.export(
                        tuKhoa,
                        hanhDong,
                        vaiTro,
                        diaChiIp,
                        tuNgay,
                        denNgay
                )
        );
    }
}