package com.oldbook.constant.auth;

public enum Role {

    KHACH_HANG("Khách hàng"),
    CHU_CUA_HANG("Chủ cửa hàng"),
    DON_VI_VAN_CHUYEN("Đơn vị vận chuyển"),
    QUAN_LY("Quản lý"),
    QUAN_TRI_VIEN("Quản trị viên");

    private final String tenHienThi;

    Role(String tenHienThi) {
        this.tenHienThi = tenHienThi;
    }

    public String getDisplayName() {
        return tenHienThi;
    }

    public String getAuthority() {
        return "ROLE_" + name();
    }
}