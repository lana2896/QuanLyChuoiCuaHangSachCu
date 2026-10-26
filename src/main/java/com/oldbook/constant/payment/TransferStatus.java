package com.oldbook.constant.payment;

public enum TransferStatus {
    /** Chưa biết chuyển vào tài khoản nào: chờ khách nhập tài khoản nhận hoàn tiền. */
    CHO_THONG_TIN,
    /** Đã đủ thông tin, chờ Quản lý chuyển khoản. */
    CHO_XU_LY,
    /** Quản lý đã chuyển khoản và xác nhận. */
    DA_CHUYEN
}
