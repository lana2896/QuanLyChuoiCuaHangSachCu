package com.oldbook.dto.management;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Số liệu thống kê vận hành. Quản lý xem để hành động; Admin dùng cùng số liệu để xem tổng và xuất CSV. */
public final class StatsDto {

    private StatsDto() {
    }

    /**
     * @param sanDangGiu khách đã trả vào - đã hoàn - đã quyết toán chi nhánh - chiết khấu - phí ship của đơn đã giao
     */
    public record DongTienSan(
            BigDecimal khachDaTra,
            BigDecimal daHoan,
            BigDecimal daChiCuaHang,
            BigDecimal hoaHong,
            BigDecimal phiShipDonDaGiao,
            BigDecimal sanDangGiu,
            long bankTransactionMismatch
    ) {
    }

    public record CuaHangThongKe(
            Integer maCH,
            String tenCuaHang,
            BigDecimal revenue,
            long soDonHoanTat,
            long tongDon,
            long soDonHuyBoiShop,
            // % đơn bị chính cửa hàng hủy trên tổng đơn đặt trong kỳ
            BigDecimal tyLeHuyBoiShop,
            // Số giờ trung bình từ lúc khách đặt đến lúc cửa hàng xác nhận; null nếu chưa có đơn nào được xác nhận
            BigDecimal gioXacNhanTrungBinh,
            // Trung bình số sao các đánh giá sách của cửa hàng; null nếu chưa có đánh giá
            BigDecimal diemDanhGia,
            long soDanhGia
    ) {
    }

    public record DonQuaHan(
            Integer maDHCH,
            Integer maDH,
            Integer maCH,
            String tenCuaHang,
            LocalDateTime ngayDat,
            long gioChoXacNhan
    ) {
    }

    public record VanChuyenThongKe(
            String tenDonVi,
            long soDon,
            long giaoThanhCong,
            long hoanTra,
            BigDecimal tyLeThanhCong,
            BigDecimal tyLeHoan,
            BigDecimal gioGiaoTrungBinh
    ) {
    }

    public record HuyTheoBen(String ben, long soDon) {
    }

    public record HoanTheoTuan(LocalDate tuNgay, BigDecimal soTien, long soKhoan) {
    }

    public record HuyHoan(
            List<HuyTheoBen> huyTheoBen,
            List<HoanTheoTuan> hoanTheoTuan,
            BigDecimal gioXuLyHoanTrungBinh
    ) {
    }

    public record DanhMucThongKe(
            Integer maDM,
            String tenDanhMuc,
            BigDecimal revenue,
            BigDecimal hoaHong,
            BigDecimal tyLeDangApDung
    ) {
    }

    public record KiemDuyetThongKe(long cuaHangCho, long sachCho, BigDecimal tyLeTuChoi) {
    }

    public record ThongKe(
            LocalDate tuNgay,
            LocalDate denNgay,
            DongTienSan dongTien,
            List<CuaHangThongKe> cuaHang,
            List<DonQuaHan> donQuaHan,
            List<VanChuyenThongKe> vanChuyen,
            HuyHoan huyHoan,
            List<DanhMucThongKe> danhMuc,
            KiemDuyetThongKe kiemDuyet
    ) {
    }
}
