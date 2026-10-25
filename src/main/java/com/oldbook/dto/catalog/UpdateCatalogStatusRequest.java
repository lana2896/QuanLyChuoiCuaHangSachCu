package com.oldbook.dto.catalog;

import jakarta.validation.constraints.NotBlank;

public record UpdateCatalogStatusRequest(
        @NotBlank(message = "Trạng thái bán không được để trống") String trangThaiBan
) {
}
