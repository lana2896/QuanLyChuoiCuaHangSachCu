package com.oldbook.dto.store;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateBookRequest(
        @NotNull(message = "Tựa sách không được để trống") @Positive Integer maTuaSach,
        @NotNull(message = "Độ mới không được để trống")
        @DecimalMin(value = "1", message = "Độ mới phải từ 1 đến 100 phần trăm")
        @DecimalMax("100") @Digits(integer = 3, fraction = 2) BigDecimal doMoiPercent,
        @Size(max = 255) String tinhTrangVatLy,
        @NotNull(message = "Số lượng tồn không được để trống")
        @Min(value = 0, message = "Số lượng tồn không được âm") Integer soLuongTon
) {
}
