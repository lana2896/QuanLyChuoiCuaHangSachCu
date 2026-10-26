package com.oldbook.dto.inventory;

import com.oldbook.constant.inventory.StockTransferStatus;
import com.oldbook.entity.catalog.Book;
import com.oldbook.entity.inventory.StockTransfer;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Một phiếu điều chuyển. chieu cho biết phiếu nhìn từ chi nhánh đang xem:
 * DI là phiếu mình gửi, DEN là phiếu gửi tới mình; null khi Quản lý xem toàn chuỗi.
 */
public record StockTransferResponse(
        Integer maPhieu,
        Integer maSachNguon,
        Integer maSachDich,
        String tenSach,
        String tacGia,
        String hinhAnhUrl,
        BigDecimal doMoiPercent,
        String tinhTrangVatLy,
        BigDecimal giaBanCu,
        Integer soLuong,
        Integer maCHNguon,
        String tenCuaHangNguon,
        Integer maCHDich,
        String tenCuaHangDich,
        String trangThai,
        String tenTrangThai,
        String ghiChu,
        String lyDoTuChoi,
        LocalDateTime ngayTao,
        LocalDateTime ngayXuLy,
        String chieu
) {
    public static StockTransferResponse from(StockTransfer phieu, Integer maCHDangXem) {
        Book sach = phieu.getSachNguon();
        StockTransferStatus status = StockTransferStatus.fromName(phieu.getTrangThai());
        String anh = sach.getHinhAnhs() != null && !sach.getHinhAnhs().isEmpty()
                ? sach.getHinhAnhs().get(0).getHinhAnhUrl()
                : sach.getHinhAnhUrl();
        Integer maCHNguon = phieu.getCuaHangNguon().getMaCH();
        Integer maCHDich = phieu.getCuaHangDich().getMaCH();
        String chieu = maCHDangXem == null ? null
                : maCHDangXem.equals(maCHNguon) ? "DI" : maCHDangXem.equals(maCHDich) ? "DEN" : null;
        return new StockTransferResponse(
                phieu.getMaPhieu(), sach.getMaSach(),
                phieu.getSachDich() == null ? null : phieu.getSachDich().getMaSach(),
                sach.getTenSach(), sach.getTacGia(), anh, sach.getDoMoiPercent(), sach.getTinhTrangVatLy(),
                sach.getGiaBanCu(), phieu.getSoLuong(),
                maCHNguon, phieu.getCuaHangNguon().getTenCuaHang(),
                maCHDich, phieu.getCuaHangDich().getTenCuaHang(),
                phieu.getTrangThai(), status == null ? phieu.getTrangThai() : status.getDisplayName(),
                phieu.getGhiChu(), phieu.getLyDoTuChoi(), phieu.getNgayTao(), phieu.getNgayXuLy(), chieu);
    }
}
