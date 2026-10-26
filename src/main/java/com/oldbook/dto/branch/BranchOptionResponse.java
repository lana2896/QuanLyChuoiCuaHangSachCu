package com.oldbook.dto.branch;

/** Chi nhánh để chọn trong danh sách (ví dụ chi nhánh nhận sách khi điều chuyển kho). */
public record BranchOptionResponse(Integer maCH, String tenCuaHang, String diaChiCH) {
}
