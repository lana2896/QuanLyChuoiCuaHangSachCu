package com.oldbook.dto.catalog;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record BookTitleRequest(
        @NotNull(message = "Danh mục không được để trống") @Positive Integer maDM,
        @NotBlank(message = "Tên sách không được để trống") @Size(max = 255) String tenSach,
        @NotBlank(message = "Tác giả không được để trống") @Size(max = 255) String tacGia,
        @NotBlank(message = "Nhà xuất bản không được để trống") @Size(max = 255) String nhaXuatBan,
        @NotNull(message = "Năm xuất bản không được để trống") @Min(1) @Max(9999) Integer namXuatBan,
        @NotNull(message = "Giá gốc không được để trống") @DecimalMin(value = "0.01", message = "Giá gốc phải lớn hơn 0")
        @Digits(integer = 13, fraction = 2) BigDecimal giaGoc,
        @NotBlank(message = "Mô tả chi tiết không được để trống") @Size(max = 10000) String moTaChiTiet,
        @NotBlank(message = "Hình ảnh không được để trống") @Size(max = 500) String hinhAnhUrl
) {
}
