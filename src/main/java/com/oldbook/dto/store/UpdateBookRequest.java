package com.oldbook.dto.store;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record UpdateBookRequest(
        @NotNull(message = "Độ mới không được để trống")
        @DecimalMin(value = "1", message = "Độ mới phải từ 1 đến 100 phần trăm")
        @DecimalMax("100") @Digits(integer = 3, fraction = 2) BigDecimal doMoiPercent,
        @Size(max = 255) String tinhTrangVatLy
) {
}
