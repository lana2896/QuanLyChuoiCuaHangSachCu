package com.oldbook.dto.store;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateStoreRequest(
        @Size(max = 1000) String moTa,
        @NotNull(message = "Tỉnh/Thành phố không được để trống") Integer maTinh,
        @NotNull(message = "Phường/Xã không được để trống") Integer maXa,
        @NotBlank(message = "Địa chỉ chi tiết không được để trống") @Size(max = 500) String diaChiChiTiet,
        @NotBlank(message = "Số điện thoại cửa hàng không được để trống")
        @Pattern(regexp = "^0\\d{9,10}$", message = "Số điện thoại cửa hàng không hợp lệ") String soDienThoaiCH,
        // Tài khoản nhận tiền trụ sở chi cho đơn khách đã chuyển khoản. Form đăng ký bắt buộc nhập;
        // API vẫn nhận yêu cầu để trống cả ba, chi nhánh bổ sung sau qua PUT /api/store/me/bank-account.
        @Pattern(regexp = "[0-9]{6}", message = "Mã ngân hàng không hợp lệ") String maNganHang,
        @Pattern(regexp = "[0-9A-Za-z]{4,30}", message = "Số tài khoản chỉ gồm chữ và số, từ 4 đến 30 ký tự") String soTaiKhoan,
        @Size(max = 255) String tenChuTaiKhoan
) {
}
