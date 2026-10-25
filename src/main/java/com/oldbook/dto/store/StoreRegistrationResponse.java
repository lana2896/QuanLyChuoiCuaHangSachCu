package com.oldbook.dto.store;

/**
 * Kết quả khi khách hàng tự đăng ký gian hàng.
 * maTK là tài khoản Chủ cửa hàng vừa tạo: trình duyệt dùng nó để chuyển sang tài khoản đó.
 */
public record StoreRegistrationResponse(
        Integer maTK,
        StoreResponse cuaHang
) {
}
