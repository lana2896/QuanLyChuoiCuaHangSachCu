package com.oldbook.constant.inventory;

/**
 * Trạng thái phiếu điều chuyển sách giữa hai chi nhánh.
 * Sách rời kho chi nhánh gửi ngay khi lập phiếu (CHO_NHAN); bị từ chối hoặc hủy thì trả lại kho gửi.
 */
public enum StockTransferStatus {

    CHO_NHAN("Đang chuyển, chờ chi nhánh nhận"),
    DA_NHAN("Đã nhập kho chi nhánh nhận"),
    TU_CHOI("Chi nhánh nhận từ chối"),
    DA_HUY("Chi nhánh gửi đã hủy");

    private final String tenHienThi;

    StockTransferStatus(String tenHienThi) {
        this.tenHienThi = tenHienThi;
    }

    public String getDisplayName() {
        return tenHienThi;
    }

    public static StockTransferStatus fromName(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
