package com.oldbook.service.admin;

import com.oldbook.dto.admin.AdminAuditLogResponse;
import com.oldbook.dto.admin.AdminAuditLogSummaryResponse;
import com.oldbook.dto.catalog.PageResponse;
import com.oldbook.entity.system.SystemLog;
import com.oldbook.repository.system.SystemLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminAuditLogService {

    private final SystemLogRepository systemLogRepository;


    /**
     * =========================================================
     * Tìm kiếm nhật ký
     * =========================================================
     */
    @Transactional(readOnly = true)
    public PageResponse<AdminAuditLogResponse> search(
            String tuKhoa,
            String hanhDong,
            String vaiTro,
            String diaChiIp,
            LocalDate tuNgay,
            LocalDate denNgay,
            int page,
            int size
    ) {

        int safePage =
                Math.max(
                        page,
                        0
                );


        int safeSize =
                normalizePageSize(size);


        String keyword =
                tuKhoa == null
                        ? ""
                        : tuKhoa.trim();


        String safeHanhDong =
                normalizeFilter(
                        hanhDong
                );


        String safeVaiTro =
                normalizeFilter(
                        vaiTro
                );


        String safeIp =
                normalizeFilter(
                        diaChiIp
                );


        LocalDateTime from =
                tuNgay == null
                        ? null
                        : tuNgay.atStartOfDay();


        /*
         * denNgay là ngày cuối mà người dùng chọn.
         *
         * Ví dụ:
         * 15/10/2026
         *
         * → < 16/10/2026 00:00:00
         *
         * nên toàn bộ ngày 15 vẫn được lấy.
         */

        LocalDateTime to =
                denNgay == null
                        ? null
                        : denNgay
                        .plusDays(1)
                        .atStartOfDay();


        Integer numericKeyword =
                parseIntegerOrNull(
                        keyword
                );


        Pageable pageable =
                PageRequest.of(
                        safePage,
                        safeSize
                );


        Page<SystemLog> result =
                systemLogRepository.searchAdminLogs(
                        keyword,
                        numericKeyword,
                        safeHanhDong,
                        safeVaiTro,
                        safeIp,
                        from,
                        to,
                        pageable
                );


        Page<AdminAuditLogResponse> mapped =
                result.map(
                        this::toResponse
                );


        return PageResponse.of(
                mapped
        );
    }


    /**
     * =========================================================
     * Xuất toàn bộ log theo bộ lọc
     * =========================================================
     */
    @Transactional(readOnly = true)
    public List<AdminAuditLogResponse> export(
            String tuKhoa,
            String hanhDong,
            String vaiTro,
            String diaChiIp,
            LocalDate tuNgay,
            LocalDate denNgay
    ) {

        String keyword =
                tuKhoa == null
                        ? ""
                        : tuKhoa.trim();


        String safeHanhDong =
                normalizeFilter(
                        hanhDong
                );


        String safeVaiTro =
                normalizeFilter(
                        vaiTro
                );


        String safeIp =
                normalizeFilter(
                        diaChiIp
                );


        LocalDateTime from =
                tuNgay == null
                        ? null
                        : tuNgay.atStartOfDay();


        LocalDateTime to =
                denNgay == null
                        ? null
                        : denNgay
                        .plusDays(1)
                        .atStartOfDay();


        Integer numericKeyword =
                parseIntegerOrNull(
                        keyword
                );


        Page<SystemLog> result =
                systemLogRepository.searchAdminLogs(
                        keyword,
                        numericKeyword,
                        safeHanhDong,
                        safeVaiTro,
                        safeIp,
                        from,
                        to,
                        Pageable.unpaged()
                );


        return result.getContent()
                .stream()
                .map(this::toResponse)
                .toList();
    }


    /**
     * =========================================================
     * KPI
     * =========================================================
     */
    @Transactional(readOnly = true)
    public AdminAuditLogSummaryResponse getSummary() {

        LocalDateTime startOfToday =
                LocalDate
                        .now()
                        .atStartOfDay();


        long actionsToday =
                systemLogRepository
                        .countByThoiGianGreaterThanEqual(
                                startOfToday
                        );


        long lockUnlockToday =
                systemLogRepository
                        .countLockActionsSince(
                                startOfToday
                        );


        // THAY_DOI_VAI_TRO là hành động cũ (trước khi chuyển sang thêm vai trò), vẫn tính để số liệu cũ không lệch.
        long roleChangesToday =
                systemLogRepository
                        .countByHanhDongAndThoiGianGreaterThanEqual(
                                "THEM_VAI_TRO",
                                startOfToday
                        )
                        + systemLogRepository
                        .countByHanhDongAndThoiGianGreaterThanEqual(
                                "THAY_DOI_VAI_TRO",
                                startOfToday
                        );


        long distinctIpsToday =
                systemLogRepository
                        .countDistinctIpSince(
                                startOfToday
                        );


        return AdminAuditLogSummaryResponse
                .builder()
                .hanhDongHomNay(
                        actionsToday
                )
                .khoaMoKhoaHomNay(
                        lockUnlockToday
                )
                .phanQuyenHomNay(
                        roleChangesToday
                )
                .soIpGhiNhanHomNay(
                        distinctIpsToday
                )
                .build();
    }


    /**
     * =========================================================
     * Mapping entity -> response
     * =========================================================
     */
    private AdminAuditLogResponse toResponse(
            SystemLog log
    ) {

        Integer maTK = null;

        String vaiTro = null;

        String tenNguoiThucHien = null;

        String emailNguoiThucHien = null;


        if (log.getTaiKhoan() != null) {

            maTK =
                    log.getTaiKhoan()
                            .getMaTK();


            vaiTro =
                    log.getTaiKhoan()
                            .getVaiTro();


            if (
                    log.getTaiKhoan()
                            .getNguoiDung() != null
            ) {

                tenNguoiThucHien =
                        log.getTaiKhoan()
                                .getNguoiDung()
                                .getHoTen();


                emailNguoiThucHien =
                        log.getTaiKhoan()
                                .getNguoiDung()
                                .getEmail();

            }
        }


        return AdminAuditLogResponse
                .builder()

                .maNhatKy(
                        log.getMaNhatKy()
                )

                .maTK(
                        maTK
                )

                .vaiTro(
                        vaiTro
                )

                .tenNguoiThucHien(
                        tenNguoiThucHien
                )

                .emailNguoiThucHien(
                        emailNguoiThucHien
                )

                .hanhDong(
                        log.getHanhDong()
                )

                .loaiDoiTuong(
                        log.getLoaiDoiTuong()
                )

                .maDoiTuong(
                        log.getMaDoiTuong()
                )

                .moTa(
                        log.getMoTa()
                )

                .diaChiIp(
                        log.getDiaChiIp()
                )

                .thoiGian(
                        log.getThoiGian()
                )

                .build();
    }


    private String normalizeFilter(
            String value
    ) {

        if (value == null) {
            return null;
        }


        String result =
                value.trim();


        return result.isBlank()
                ? null
                : result;
    }


    private Integer parseIntegerOrNull(
            String value
    ) {

        if (
                value == null ||
                        value.isBlank()
        ) {

            return null;
        }


        try {

            return Integer.valueOf(
                    value
            );

        } catch (NumberFormatException e) {

            return null;

        }
    }


    private int normalizePageSize(
            int size
    ) {

        /*
         * Giao diện mặc định 20 dòng/trang.
         *
         * Giới hạn để tránh request quá lớn.
         */

        if (size <= 0) {
            return 20;
        }


        return Math.min(
                size,
                100
        );
    }
}