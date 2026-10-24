package com.oldbook.constant.notification;

public enum NotificationLevel {

    // Cần xử lý ngay
    NGHIEM_TRONG("Nghiêm trọng"),

    // Cần kiểm tra sớm
    QUAN_TRONG("Quan trọng"),

    // Chỉ cần biết
    THONG_TIN("Thông tin");

    private final String tenHienThi;

    NotificationLevel(String tenHienThi) {
        this.tenHienThi = tenHienThi;
    }

    public String getDisplayName() {
        return tenHienThi;
    }
}
