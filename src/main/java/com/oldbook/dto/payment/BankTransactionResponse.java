package com.oldbook.dto.payment;

import com.oldbook.entity.payment.BankTransaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BankTransactionResponse(
        Integer maGiaoDich,
        String maGiaoDichCong,
        BigDecimal soTien,
        String noiDung,
        LocalDateTime thoiGianGiaoDich,
        String tenNguoiChuyen,
        String soTaiKhoanChuyen,
        String maThanhToan,
        String ketQua,
        String ghiChu,
        LocalDateTime ngayNhan
) {

    public static BankTransactionResponse from(BankTransaction giaoDich) {
        return new BankTransactionResponse(
                giaoDich.getMaGiaoDich(),
                giaoDich.getMaGiaoDichCong(),
                giaoDich.getSoTien(),
                giaoDich.getNoiDung(),
                giaoDich.getThoiGianGiaoDich(),
                giaoDich.getTenNguoiChuyen(),
                giaoDich.getSoTaiKhoanChuyen(),
                giaoDich.getPhien() == null ? null : giaoDich.getPhien().getMaThanhToan(),
                giaoDich.getKetQua(),
                giaoDich.getGhiChu(),
                giaoDich.getNgayNhan()
        );
    }
}
