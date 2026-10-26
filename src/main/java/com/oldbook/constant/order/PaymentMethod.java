package com.oldbook.constant.order;

/** Phương thức thanh toán của đơn tổng (cột phuong_thuc_tt). */
public enum PaymentMethod {
    /** Đơn vị vận chuyển thu tiền của khách khi giao hàng. */
    COD,
    /** Khách chuyển khoản vào tài khoản của sàn; đơn chỉ được tạo sau khi tiền về. */
    CHUYEN_KHOAN;

    public static boolean isPrepaid(String phuongThuc) {
        return CHUYEN_KHOAN.name().equals(phuongThuc);
    }
}
