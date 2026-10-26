package com.oldbook.dto.management;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Các dạng dữ liệu trả về / nhận vào của API /api/management/**. */
public final class ManagementDto {

    private ManagementDto() {
    }

    // ---------------- Tổng quan ----------------

    /** Số việc đang chờ, dùng cho huy hiệu ở sidebar. */
    public record HuyHieu(long kiemDuyet, long thanhToan) {
    }

    public record ViecCho(String loai, String tieuDe, String moTa, String duongDan, String nut, LocalDateTime thoiGian) {
    }

    public record DongTien(BigDecimal khachDaTra, BigDecimal daHoan, BigDecimal daChiCuaHang, BigDecimal hoaHong) {
    }

    public record DoiTacVanChuyen(String tenDonVi, long tongDon, long giaoThanhCong, long hoanTra) {
    }

    public record TongQuan(
            long choDuyetCuaHang,
            long choDuyetSach,
            long hoanTienCho,
            long hoanTienChoKhachKhai,
            long cuaHangChoChi,
            long khoanChoChi,
            BigDecimal tongTienChoChi,
            BigDecimal hoaHongThangNay,
            long bankTransactionMismatch,
            HuyHieu huyHieu,
            List<ViecCho> hangDoi,
            DongTien dongTien,
            List<DoiTacVanChuyen> vanChuyen,
            LocalDateTime capNhatLuc
    ) {
    }

    // ---------------- Chi trả cho cửa hàng ----------------

    public record DonChi(
            Integer maYeuCau,
            Integer maDHCH,
            Integer maDH,
            LocalDateTime ngayGiao,
            BigDecimal tienHang,
            BigDecimal giamGiaShop,
            BigDecimal hoaHong,
            BigDecimal thucNhan,
            String trangThai,
            String maDotChi
    ) {
    }

    public record NhomChi(
            Integer maCH,
            String tenCuaHang,
            String tenChuShop,
            String soDienThoai,
            boolean coTaiKhoan,
            String maNganHang,
            String soTaiKhoan,
            String tenChuTaiKhoan,
            int soDon,
            BigDecimal tongTienHang,
            BigDecimal tongHoaHong,
            BigDecimal tongThucNhan,
            LocalDateTime daNhacLuc,
            List<DonChi> donCon
    ) {
    }

    public record DotChiRequest(List<Integer> maYeuCau) {
    }

    public record DotChiXacNhanRequest(String maDotChi, List<Integer> maYeuCau) {
    }

    public record TaiKhoanNhan(String maNganHang, String soTaiKhoan, String tenChuTaiKhoan) {
    }

    public record DotChiXemTruoc(
            String maDotChi,
            Integer maCH,
            String tenCuaHang,
            int soDon,
            BigDecimal tongTien,
            String qrUrl,
            TaiKhoanNhan taiKhoan
    ) {
    }

    public record KetQuaNhac(LocalDateTime daNhacLuc) {
    }

    // ---------------- Giao dịch ngân hàng ----------------

    public record TomTatGiaoDich(long tongSo, BigDecimal tongTienKhop, BigDecimal tongTienLech) {
    }

    /** Một trang giao dịch kèm 3 số tóm tắt tính trên toàn bộ kết quả lọc (không chỉ trang này). */
    public record GiaoDichTrang(
            List<com.oldbook.dto.payment.BankTransactionResponse> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            TomTatGiaoDich tomTat
    ) {
    }

    // ---------------- Hoa hồng ----------------

    public record HoaHongDanhMuc(
            Integer maDM,
            String tenDanhMuc,
            BigDecimal tyLe,
            boolean macDinh,
            long soCuaHang,
            LocalDateTime apDungTu,
            LocalDateTime suaLanCuoi,
            String nguoiSua,
            String ghiChu
    ) {
    }

    public record HoaHongLichSu(
            Integer maCauHinh,
            BigDecimal tyLe,
            String trangThai,
            LocalDateTime ngayBatDau,
            LocalDateTime ngayKetThuc,
            String nguoiSua,
            String ghiChu
    ) {
    }

    public record DoiHoaHongRequest(BigDecimal tyLe, String ghiChu) {
    }
}
