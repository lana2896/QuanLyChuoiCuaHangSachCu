package com.oldbook.dto.store;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateStockRequest(
        @NotNull(message = "Số lượng tồn không được để trống")
        @Min(value = 0, message = "Số lượng tồn không được âm") Integer soLuongTon
) {
}
