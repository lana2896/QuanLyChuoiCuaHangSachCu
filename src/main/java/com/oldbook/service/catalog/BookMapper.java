package com.oldbook.service.catalog;

import com.oldbook.dto.catalog.SachResponse;
import com.oldbook.entity.catalog.Sach;

public final class SachMapper {
    private SachMapper() {
    }

    public static SachResponse toResponse(Sach sach) {
        return toResponse(sach, true);
    }

    public static SachResponse toPublicResponse(Sach sach) {
        return toResponse(sach, false);
    }

    private static SachResponse toResponse(Sach sach, boolean includeRejectionReason) {
        return new SachResponse(
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
