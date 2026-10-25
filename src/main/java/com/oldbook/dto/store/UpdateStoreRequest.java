package com.oldbook.dto.store;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateStoreRequest(
        @Size(max = 1000) String moTa,
        @NotNull(message = "Tỉnh/Thành phố không được để trống") Integer maTinh,
        @NotNull(message = "Phường/Xã không được để trống") Integer maXa,
        @NotBlank(message = "Địa chỉ chi tiết không được để trống") @Size(max = 500) String diaChiChiTiet,
        @NotBlank(message = "Số điện thoại cửa hàng không được để trống") @Size(max = 20) String soDienThoaiCH
) {
}
