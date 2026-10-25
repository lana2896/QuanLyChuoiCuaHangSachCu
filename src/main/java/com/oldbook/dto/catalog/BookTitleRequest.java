package com.oldbook.dto.catalog;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record BookTitleRequest(
        @NotNull(message = "Danh mục không được để trống") @Positive Integer maDM,
        @NotBlank(message = "Tên sách không được để trống") @Size(max = 255) String tenSach,
        @Size(max = 255) String tacGia,
        @Size(max = 255) String nhaXuatBan,
        @Min(1) @Max(9999) Integer namXuatBan,
        @NotNull(message = "Giá gốc không được để trống") @DecimalMin(value = "0.01", message = "Giá gốc phải lớn hơn 0")
        @Digits(integer = 13, fraction = 2) BigDecimal giaGoc,
        @Size(max = 10000) String moTaChiTiet,
        @Size(max = 500) String hinhAnhUrl
) {
}
