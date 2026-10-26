package com.oldbook.dto.inventory;

import java.math.BigDecimal;
import java.util.List;

/**
 * Tồn kho toàn chuỗi của một tựa sách: tổng số cuốn và phân bổ theo từng chi nhánh.
 */
public record ChainStockResponse(
        Integer maTuaSach,
        String tenSach,
        String tacGia,
        long tongTon,
        int soChiNhanh,
        List<Item> chiNhanh
) {
    /** Một bản sách (độ mới, giá riêng) đang nằm ở một chi nhánh. */
    public record Item(
            Integer maCH,
            String tenCuaHang,
            Integer maSach,
            BigDecimal doMoiPercent,
            BigDecimal giaBanCu,
            Integer soLuongTon,
            String trangThaiBan
    ) {
    }
}
