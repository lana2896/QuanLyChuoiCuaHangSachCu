package com.oldbook.dto.branch;

import com.oldbook.dto.store.StoreResponse;
import com.oldbook.entity.catalog.Store;

import java.time.LocalDateTime;

/**
 * Thông tin công khai của một chi nhánh trong chuỗi, dùng cho trang Hệ thống chi nhánh.
 * Không chứa tài khoản ngân hàng hay trạng thái duyệt: khách chỉ thấy chi nhánh đang hoạt động.
 * soDauSach là số bản sách đang bán, tongTonKho là tổng số cuốn còn trong kho của chi nhánh.
 */
public record BranchResponse(
        Integer maCH,
        String tenCuaHang,
        Integer soChiNhanh,
        String moTa,
        String diaChiCH,
        Integer maTinh,
        String tenTinh,
        String soDienThoaiCH,
        String gioHoatDong,
        long soDauSach,
        long tongTonKho,
        LocalDateTime ngayTao
) {
    public static BranchResponse from(Store store, long soDauSach, long tongTonKho) {
        StoreResponse info = StoreResponse.from(store);
        return new BranchResponse(info.maCH(), info.tenCuaHang(), info.soChiNhanh(), info.moTa(), info.diaChiCH(),
                info.maTinh(), info.tenTinh(), info.soDienThoaiCH(), info.gioHoatDong(),
                soDauSach, tongTonKho, info.ngayTao());
    }
}
