package com.oldbook.dto.store;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record UpdateBookRequest(
        @NotNull(message = "Danh mục không được để trống") @Positive Integer maDM,
        @NotBlank(message = "Tên sách không được để trống") @Size(max = 255) String tenSach,
        @Size(max = 255) String tacGia,
        @Size(max = 255) String nhaXuatBan,
        @Min(1) @Max(9999) Integer namXuatBan,
        @DecimalMin("0") @Digits(integer = 13, fraction = 2) BigDecimal giaBia,
        @NotNull(message = "Giá bán không được để trống") @DecimalMin("0.01")
        @Digits(integer = 13, fraction = 2) BigDecimal giaBanCu,
        @DecimalMin("0") @DecimalMax("100") @Digits(integer = 3, fraction = 2) BigDecimal doMoiPercent,
        @Size(max = 255) String tinhTrangVatLy,
        @Size(max = 10000) String moTaChiTiet,
        @Size(max = 500) String hinhAnhUrl
) {
}
