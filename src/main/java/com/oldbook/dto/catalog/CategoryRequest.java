package com.oldbook.dto.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank(message = "Tên danh mục không được để trống") @Size(max = 255) String tenDanhMuc,
        @NotBlank(message = "Mô tả không được để trống") @Size(max = 500) String moTa
) {
}
