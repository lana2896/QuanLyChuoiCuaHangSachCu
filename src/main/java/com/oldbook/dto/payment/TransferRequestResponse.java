package com.oldbook.dto.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransferRequestResponse(
        Integer maYeuCau,
        String loai,
        String trangThai,
        BigDecimal soTien,
        // Nội dung Quản lý ghi khi chuyển khoản, ví dụ HT15.
        String noiDungChuyenKhoan,
        Integer maDH,
        Integer maDHCH,
        String lyDo,
        String tenNguoiNhan,
        String emailNguoiNhan,
        String soDienThoaiNguoiNhan,
        String tenCuaHang,
        String maNganHang,
        String soTaiKhoan,
        String tenChuTaiKhoan,
        // Mã VietQR tới tài khoản người nhận; trống khi chưa có đủ thông tin tài khoản.
        String qrUrl,
        LocalDateTime ngayTao,
        LocalDateTime ngayXuLy,
        // Bên hủy đơn (KHACH_HANG, CUA_HANG, HE_THONG) và lý do hủy; trống với khoản không do hủy đơn.
        String huyBoi,
        String lyDoHuy,
        String maDotChi
) {
}
