package com.oldbook.service.order;

import java.util.Set;

/**
 * Phát ra khi một đơn hàng con vừa được tạo hoặc đổi trạng thái (xác nhận, hủy, yêu cầu hủy, vận chuyển...).
 * Kênh đẩy (OrderChangedPusher) nghe sự kiện này sau khi giao dịch commit để các trang theo dõi đơn
 * của khách và chủ chi nhánh tự tải lại danh sách.
 *
 * @param maDH     mã đơn tổng
 * @param maDHCH   mã đơn hàng con vừa thay đổi
 * @param maTKNhan các tài khoản cần được báo: tài khoản khách mua và tài khoản chủ chi nhánh
 */
public record OrderChangedEvent(Integer maDH, Integer maDHCH, Set<Integer> maTKNhan) {
}
