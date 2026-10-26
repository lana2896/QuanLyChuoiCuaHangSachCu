package com.oldbook.dto.store;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateStoreRequest(
        @Size(max = 1000) String moTa,
        @NotNull(message = "Tỉnh/Thành phố không được để trống") Integer maTinh,
        @NotNull(message = "Phường/Xã không được để trống") Integer maXa,
        @NotBlank(message = "Địa chỉ chi tiết không được để trống") @Size(max = 500) String diaChiChiTiet,
        @NotBlank(message = "Số điện thoại cửa hàng không được để trống")
        @Pattern(regexp = "^0\\d{9,10}$", message = "Số điện thoại cửa hàng không hợp lệ") String soDienThoaiCH,
        // Giờ mở cửa hiển thị cho khách, không bắt buộc. Ví dụ "08:00 - 21:30".
        @Size(max = 100, message = "Giờ hoạt động tối đa 100 ký tự") String gioHoatDong
) {
}
