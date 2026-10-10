package com.oldbook.dto.store;

import com.oldbook.entity.catalog.Store;

import java.time.LocalDateTime;

public record StoreResponse(
        Integer maCH,
        String tenCuaHang,
        String moTa,
        String diaChiCH,
        String soDienThoaiCH,
        String trangThaiDuyet,
        LocalDateTime ngayTao,
        String lyDoTuChoi
) {
    public static StoreResponse from(Store store) {
        return new StoreResponse(store.getMaCH(), store.getTenCuaHang(), store.getMoTa(),
                store.getDiaChiCH(), store.getSoDienThoaiCH(), store.getTrangThaiDuyet(),
                store.getNgayTao(), null);
    }

    public StoreResponse withLyDoTuChoi(String lyDo) {
        return new StoreResponse(maCH, tenCuaHang, moTa, diaChiCH, soDienThoaiCH,
                trangThaiDuyet, ngayTao, lyDo);
    }
}
