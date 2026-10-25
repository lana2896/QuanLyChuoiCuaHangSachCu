package com.oldbook.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class AdminDashboardSummaryResponse {

    // Đếm từ nguoi_dung. Một người dùng có thể có nhiều tài khoản (mỗi vai trò một tài khoản).
    private long tongNguoiDung;

    // Các số còn lại đếm từ tai_khoan.
    private long tongTaiKhoan;

    private long taiKhoanHoatDong;

    private long accountLocked;

    private long taiKhoanChoXacThuc;

    private List<PhanBoVaiTro> phanBoVaiTro;

    public record PhanBoVaiTro(
            String vaiTro,
            String tenHienThi,
            long soLuong
    ) {
    }
}
