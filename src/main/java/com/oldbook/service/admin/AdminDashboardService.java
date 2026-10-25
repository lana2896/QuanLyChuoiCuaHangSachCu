package com.oldbook.service.admin;

import com.oldbook.constant.auth.AccountStatus;
import com.oldbook.constant.auth.Role;
import com.oldbook.dto.admin.AdminDashboardSummaryResponse;
import com.oldbook.dto.admin.AdminDashboardSummaryResponse.PhanBoVaiTro;
import com.oldbook.repository.admin.AdminDashboardRepository;
import com.oldbook.repository.admin.AdminDashboardRepository.CodeCount;
import com.oldbook.repository.identity.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Dashboard admin chỉ theo dõi hệ thống: người dùng, tài khoản, vai trò. Số liệu doanh thu nằm ở AdminReportService.
@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final AdminDashboardRepository adminDashboardRepository;

    @Transactional(readOnly = true)
    public AdminDashboardSummaryResponse getSummary() {

        Map<String, Long> theoTrangThai =
                toMap(adminDashboardRepository.countAccountsGroupedByStatus());

        Map<String, Long> theoVaiTro =
                toMap(adminDashboardRepository.countAccountsGroupedByRole());

        // Duyệt theo enum để thứ tự ổn định; vai trò chưa có tài khoản nào thì không hiển thị.
        List<PhanBoVaiTro> phanBoVaiTro = Arrays.stream(Role.values())
                .filter(vaiTro -> theoVaiTro.getOrDefault(vaiTro.name(), 0L) > 0)
                .map(vaiTro -> new PhanBoVaiTro(
                        vaiTro.name(),
                        vaiTro.getDisplayName(),
                        theoVaiTro.get(vaiTro.name())
                ))
                .toList();

        return AdminDashboardSummaryResponse.builder()
                .tongNguoiDung(userRepository.count())
                .tongTaiKhoan(theoTrangThai.values().stream().mapToLong(Long::longValue).sum())
                .taiKhoanHoatDong(count(theoTrangThai, AccountStatus.HOAT_DONG))
                .accountLocked(count(theoTrangThai, AccountStatus.BI_KHOA))
                .taiKhoanChoXacThuc(count(theoTrangThai, AccountStatus.CHO_XAC_THUC))
                .phanBoVaiTro(phanBoVaiTro)
                .build();
    }

    private Map<String, Long> toMap(List<CodeCount> rows) {
        return rows.stream()
                .collect(Collectors.toMap(CodeCount::getCode, CodeCount::getTotal));
    }

    private long count(Map<String, Long> theoTrangThai, AccountStatus trangThai) {
        return theoTrangThai.getOrDefault(trangThai.name(), 0L);
    }
}
