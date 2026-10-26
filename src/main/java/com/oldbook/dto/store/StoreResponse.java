package com.oldbook.dto.store;

import com.oldbook.entity.catalog.Store;
import com.oldbook.entity.location.PhuongXa;
import com.oldbook.entity.location.TinhThanh;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Thông tin cửa hàng (chi nhánh) trả về cho trình duyệt.
 * diaChiCH là địa chỉ đầy đủ đã ghép sẵn để hiển thị;
 * maTinh, maXa, diaChiChiTiet dùng để điền lại form sửa thông tin.
 */
public record StoreResponse(
        Integer maCH,
        String tenCuaHang,
        Integer soChiNhanh,
        String moTa,
        String diaChiCH,
        Integer maTinh,
        String tenTinh,
        Integer maXa,
        String tenXa,
        String diaChiChiTiet,
        String soDienThoaiCH,
        String trangThaiDuyet,
        LocalDateTime ngayTao,
        String lyDoTuChoi,
        String maNganHang,
        String soTaiKhoan,
        String tenChuTaiKhoan,
        String gioHoatDong
) {
    public static StoreResponse from(Store store) {
        TinhThanh tinh = store.getTinh();
        PhuongXa xa = store.getXa();
        String tenTinh = tinh == null ? null : tinh.getTenTinh();
        String tenXa = xa == null ? null : xa.getTenXa();
        // Ghép địa chỉ an toàn: cửa hàng tạo trước khi có bảng địa giới có thể thiếu tỉnh/xã.
        String diaChiCH = Stream.of(store.getDiaChiChiTiet(), tenXa, tenTinh)
                .filter(Objects::nonNull)
                .filter(part -> !part.isBlank())
                .collect(Collectors.joining(", "));
        return new StoreResponse(store.getMaCH(), store.getTenCuaHang(), store.getSoChiNhanh(), store.getMoTa(),
                diaChiCH.isEmpty() ? null : diaChiCH,
                tinh == null ? null : tinh.getMaTinh(), tenTinh,
                xa == null ? null : xa.getMaXa(), tenXa,
                store.getDiaChiChiTiet(), store.getSoDienThoaiCH(), store.getTrangThaiDuyet(),
                store.getNgayTao(), null,
                store.getMaNganHang(), store.getSoTaiKhoan(), store.getTenChuTaiKhoan(),
                store.getGioHoatDong());
    }

    public StoreResponse withLyDoTuChoi(String lyDo) {
        return new StoreResponse(maCH, tenCuaHang, soChiNhanh, moTa, diaChiCH, maTinh, tenTinh, maXa, tenXa,
                diaChiChiTiet, soDienThoaiCH, trangThaiDuyet, ngayTao, lyDo,
                maNganHang, soTaiKhoan, tenChuTaiKhoan, gioHoatDong);
    }
}
