package com.oldbook.service.catalog;

import com.oldbook.dto.catalog.BookResponse;
import com.oldbook.entity.catalog.Book;

public final class BookMapper {
    private BookMapper() {
    }

    public static BookResponse toResponse(Book sach) {
        return toResponse(sach, true);
    }

    public static BookResponse toPublicResponse(Book sach) {
        return toResponse(sach, false);
    }

    private static BookResponse toResponse(Book sach, boolean includeRejectionReason) {
        return new BookResponse(
                sach.getMaSach(), sach.getCuaHang().getMaCH(), sach.getCuaHang().getTenCuaHang(),
                sach.getDanhMuc().getMaDM(), sach.getDanhMuc().getTenDanhMuc(),
                sach.getTenSach(), sach.getTacGia(), sach.getNhaXuatBan(), sach.getNamXuatBan(),
                sach.getGiaBia(), sach.getGiaBanCu(), sach.getDoMoiPercent(),
                sach.getTinhTrangVatLy(), sach.getMoTaChiTiet(), sach.getSoLuongTon() == null ? 0 : sach.getSoLuongTon(),
                sach.getHinhAnhUrl(), sach.getTrangThaiDuyet(), sach.getTrangThaiBan(),
                includeRejectionReason ? sach.getLyDoTuChoi() : null, sach.getNgayTao()
        );
    }
}
