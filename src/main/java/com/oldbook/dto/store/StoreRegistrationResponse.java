package com.oldbook.dto.store;

/**
 * Kết quả khi khách hàng tự đăng ký mở chi nhánh.
 * maTK là tài khoản Chủ chi nhánh vừa tạo: trình duyệt dùng nó để chuyển sang tài khoản đó.
 */
public record StoreRegistrationResponse(
        Integer maTK,
        StoreResponse cuaHang
) {
}
