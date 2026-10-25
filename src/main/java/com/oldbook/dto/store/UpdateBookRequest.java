package com.oldbook.dto.store;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record UpdateBookRequest(
        @NotNull(message = "Độ mới không được để trống")
        @DecimalMin(value = "1", message = "Độ mới phải từ 1 đến 100 phần trăm")
        @DecimalMax("100") @Digits(integer = 3, fraction = 2) BigDecimal doMoiPercent,
        @NotBlank(message = "Tình trạng thực tế không được để trống") @Size(max = 255) String tinhTrangVatLy,
        @NotEmpty(message = "Cần ít nhất 1 ảnh sách")
        @Size(max = 10, message = "Tối đa 10 ảnh cho mỗi sách") List<@NotBlank @Size(max = 500) String> hinhAnhUrls
) {
}
