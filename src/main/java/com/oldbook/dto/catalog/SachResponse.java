package com.oldbook.dto.catalog;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SachResponse(
        Integer maSach,
        Integer maCH,
        String tenCuaHang,
        Integer maDM,
        String tenDanhMuc,
        String tenSach,
        String tacGia,
        String nhaXuatBan,
        Integer namXuatBan,
        BigDecimal giaBia,
        BigDecimal giaBanCu,
        BigDecimal doMoiPercent,
        String tinhTrangVatLy,
        String moTaChiTiet,
        Integer soLuongTon,
        String hinhAnhUrl,
        String trangThaiDuyet,
        String trangThaiBan,
        String lyDoTuChoi,
        LocalDateTime ngayTao
) {
}
