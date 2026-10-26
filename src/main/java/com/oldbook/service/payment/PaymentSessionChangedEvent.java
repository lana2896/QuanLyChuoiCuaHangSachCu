package com.oldbook.service.payment;

/**
 * Phát ra khi một phiên thanh toán đổi trạng thái (đã thanh toán, hết hạn, hủy, cần hoàn tiền).
 * Kênh đẩy (PaymentSessionPusher) nghe sự kiện này sau khi giao dịch commit để trang quét mã nhận kết quả ngay.
 *
 * @param maTK     tài khoản khách đang mở trang thanh toán
 * @param maPhien  mã phiên vừa đổi trạng thái
 * @param trangThai tên của PaymentSessionStatus mới
 */
public record PaymentSessionChangedEvent(Integer maTK, Integer maPhien, String trangThai) {
}
