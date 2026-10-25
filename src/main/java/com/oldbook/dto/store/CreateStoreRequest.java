package com.oldbook.dto.store;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateStoreRequest(
        @Size(max = 1000) String moTa,
        @NotBlank(message = "Địa chỉ cửa hàng không được để trống") @Size(max = 500) String diaChiCH,
        @NotBlank(message = "Số điện thoại cửa hàng không được để trống") @Size(max = 20) String soDienThoaiCH
) {
}
