package com.oldbook.dto.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentSessionResponse(
        Integer maPhien,
        String maThanhToan,
        BigDecimal soTien,
        String trangThai,
        LocalDateTime hetHanLuc,
        // Tính ở máy chủ để trang QR không phụ thuộc đồng hồ của trình duyệt.
        long conLaiGiay,
        String qrUrl,
        String maNganHang,
        String soTaiKhoan,
        String tenTaiKhoan,
        // Có giá trị khi tiền đã về và đơn hàng đã được tạo.
        Integer maDH
) {
}
