package com.oldbook.dto.branch;

/** Một tỉnh/thành có chi nhánh đang hoạt động, kèm số chi nhánh ở đó (bộ lọc trang Hệ thống chi nhánh). */
public record BranchProvinceResponse(Integer maTinh, String tenTinh, long soChiNhanh) {
}
