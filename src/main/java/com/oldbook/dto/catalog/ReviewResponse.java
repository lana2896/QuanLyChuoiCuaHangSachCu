package com.oldbook.dto.catalog;

import java.time.LocalDateTime;
import java.util.List;

public record ReviewResponse(
        Integer maDanhGia,
        String hoTen,
        Integer soSao,
        String binhLuan,
        LocalDateTime ngayDanhGia,
        List<ReviewMediaResponse> media
) {
    public ReviewResponse {
        media = media == null ? List.of() : List.copyOf(media);
    }

    public ReviewResponse(Integer maDanhGia, String hoTen, Integer soSao,
                           String binhLuan, LocalDateTime ngayDanhGia) {
        this(maDanhGia, hoTen, soSao, binhLuan, ngayDanhGia, List.of());
    }
}
