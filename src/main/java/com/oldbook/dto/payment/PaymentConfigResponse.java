package com.oldbook.dto.payment;

/** Trang thanh toán dùng để biết có hiện lựa chọn chuyển khoản hay không. */
public record PaymentConfigResponse(
        boolean chuyenKhoan,
        int soPhut
) {
}
