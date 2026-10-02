package com.oldbook.constant.auth;

public enum TrangThaiTaiKhoan {

    CHO_XAC_THUC("Chờ xác thực"),
    HOAT_DONG("Hoạt động"),
    BI_KHOA("Bị khóa");

    private final String tenHienThi;

    TrangThaiTaiKhoan(String tenHienThi) {
        this.tenHienThi = tenHienThi;
    }

    public String getTenHienThi() {
        return tenHienThi;
    }
}