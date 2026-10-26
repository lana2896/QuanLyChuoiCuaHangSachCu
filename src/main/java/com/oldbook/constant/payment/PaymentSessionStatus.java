package com.oldbook.constant.payment;

public enum PaymentSessionStatus {
    /** Đang giữ hàng và chờ khách chuyển khoản. */
    CHO_THANH_TOAN,
    /** Tiền đã về và đơn hàng đã được tạo. */
    DA_THANH_TOAN,
    /** Quá hạn mà chưa có tiền; hàng đã được trả về kho. */
    HET_HAN,
    /** Khách tự hủy trước khi chuyển khoản. */
    DA_HUY,
    /** Tiền đã về nhưng không tạo được đơn; trụ sở phải hoàn lại. */
    CAN_HOAN_TIEN
}
