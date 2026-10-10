package com.oldbook.constant.auth;

public enum AccountStatus {

    CHO_XAC_THUC("Chờ xác thực"),
    HOAT_DONG("Hoạt động"),
    BI_KHOA("Bị khóa");

    private final String tenHienThi;

    AccountStatus(String tenHienThi) {
        this.tenHienThi = tenHienThi;
    }

    public String getDisplayName() {
        return tenHienThi;
    }
}