package com.oldbook.constant.order;

/** Trạng thái thanh toán của đơn tổng và của đơn hàng con (COD: thu tiền khi giao thành công). */
public enum PaymentStatus {
    CHO_THANH_TOAN,
    DA_THANH_TOAN,
    /** Đơn tổng có đơn con đã giao (đã thu tiền) và đơn con chưa giao/đã hoàn. */
    THANH_TOAN_MOT_PHAN,
    /** Đơn bị hoàn/hủy hoàn toàn nên không thu được tiền. */
    KHONG_THANH_TOAN
}
