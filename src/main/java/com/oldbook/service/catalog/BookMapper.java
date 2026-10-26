package com.oldbook.service.catalog;

import com.oldbook.dto.catalog.BookResponse;
import com.oldbook.entity.catalog.Book;
import com.oldbook.entity.catalog.BookImage;

import java.util.List;

public final class BookMapper {
    private BookMapper() {
    }

    public static BookResponse toResponse(Book sach) {
        return toResponse(sach, true, List.of());
    }

    public static BookResponse toResponse(Book sach, List<String> hinhAnhUrls) {
        return toResponse(sach, true, hinhAnhUrls);
    }

    public static BookResponse toPublicResponse(Book sach) {
        return toResponse(sach, false, List.of());
    }

    private static BookResponse toResponse(Book sach, boolean includeRejectionReason, List<String> hinhAnhUrls) {
        List<String> urls = hinhAnhUrls.isEmpty() ? imageUrls(sach) : hinhAnhUrls;
        return new BookResponse(
                sach.getMaSach(), sach.getCuaHang().getMaCH(), sach.getCuaHang().getTenCuaHang(),
                sach.getDanhMuc().getMaDM(), sach.getDanhMuc().getTenDanhMuc(),
                sach.getTenSach(), sach.getTacGia(), sach.getNhaXuatBan(), sach.getNamXuatBan(),
                sach.getGiaBia(), sach.getGiaBanCu(), sach.getDoMoiPercent(),
                sach.getTinhTrangVatLy(), sach.getMoTaChiTiet(), sach.getSoLuongTon() == null ? 0 : sach.getSoLuongTon(),
                sach.getHinhAnhUrl(), sach.getTrangThaiDuyet(), sach.getTrangThaiBan(),
                includeRejectionReason ? sach.getLyDoTuChoi() : null, sach.getNgayTao(), urls
        );
    }

    private static List<String> imageUrls(Book sach) {
        List<BookImage> images = sach.getHinhAnhs();
        if (images == null) {
            return List.of();
        }
        return images.stream().map(BookImage::getHinhAnhUrl).toList();
    }
}
