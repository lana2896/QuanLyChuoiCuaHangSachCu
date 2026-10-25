package com.oldbook.dto.catalog;

import com.oldbook.entity.catalog.BookTitle;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BookTitleResponse(
        Integer maTuaSach,
        Integer maDM,
        String tenDanhMuc,
        String tenSach,
        String tacGia,
        String nhaXuatBan,
        Integer namXuatBan,
        BigDecimal giaGoc,
        String moTaChiTiet,
        String hinhAnhUrl,
        String trangThaiBan,
        LocalDateTime ngayTao
) {
    public static BookTitleResponse from(BookTitle t) {
        return new BookTitleResponse(t.getMaTuaSach(), t.getDanhMuc().getMaDM(), t.getDanhMuc().getTenDanhMuc(),
                t.getTenSach(), t.getTacGia(), t.getNhaXuatBan(), t.getNamXuatBan(), t.getGiaGoc(),
                t.getMoTaChiTiet(), t.getHinhAnhUrl(), t.getTrangThaiBan(), t.getNgayTao());
    }
}
