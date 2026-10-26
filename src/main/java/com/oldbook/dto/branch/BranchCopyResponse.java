package com.oldbook.dto.branch;

import java.math.BigDecimal;

/**
 * Một bản của cùng tựa sách đang bán ở chi nhánh khác.
 * Trang chi tiết sách dùng để báo khách "cuốn này còn ở những chi nhánh nào".
 */
public record BranchCopyResponse(
        Integer maSach,
        Integer maCH,
        String tenCuaHang,
        String diaChiCH,
        BigDecimal giaBanCu,
        BigDecimal doMoiPercent,
        String tinhTrangVatLy,
        Integer soLuongTon
) {
}
