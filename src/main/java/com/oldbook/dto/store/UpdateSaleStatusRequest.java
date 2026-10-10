package com.oldbook.dto.store;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateSaleStatusRequest(
        @NotBlank(message = "Trạng thái bán không được để trống")
        @Pattern(regexp = "DANG_BAN|NGUNG_BAN", message = "Trạng thái bán phải là DANG_BAN hoặc NGUNG_BAN")
        String trangThaiBan
) {
}
