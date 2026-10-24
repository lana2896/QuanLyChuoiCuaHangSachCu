package com.oldbook.dto.catalog;

import java.time.LocalDateTime;

public record ReviewResponse(
        Integer maDanhGia,
        String hoTen,
        Integer soSao,
        String binhLuan,
        LocalDateTime ngayDanhGia
) {
}
