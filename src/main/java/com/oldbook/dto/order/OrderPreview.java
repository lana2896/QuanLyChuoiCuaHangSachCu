package com.oldbook.dto.order;

import java.math.BigDecimal;
import java.util.List;

/** Kết quả kiểm tra và tính tiền một yêu cầu đặt hàng khi chưa lưu gì. */
public record OrderPreview(
        BigDecimal tongThanhToan,
        List<Line> lines
) {

    public record Line(
            Integer maSach,
            int soLuong
    ) {
    }
}
