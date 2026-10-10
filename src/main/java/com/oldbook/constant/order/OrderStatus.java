package com.oldbook.constant.order;

public enum OrderStatus {

    CHO_XU_LY("Chờ xử lý"),
    DA_XAC_NHAN("Đã xác nhận"),
    DANG_LAY_HANG("Đang lấy hàng"),
    DA_LAY_HANG("Đã lấy hàng"),
    DANG_GIAO("Đang giao"),
    DA_GIAO("Đã giao"),
    DA_HOAN_TRA("Đã hoàn trả"),
    DA_HUY("Đã hủy");

    private final String tenHienThi;

    OrderStatus(String tenHienThi) {
        this.tenHienThi = tenHienThi;
    }

    public String getDisplayName() {
        return tenHienThi;
    }

    public boolean isFinal() {
        return this == DA_GIAO || this == DA_HOAN_TRA || this == DA_HUY;
    }

    public static OrderStatus fromName(String value) {
        if (value == null) {
            return null;
        }
        try {
            return valueOf(value.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
