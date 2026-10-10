package com.oldbook.dto.order;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/** Thông tin giao hàng của đơn hàng con. Chỉ có sau khi cửa hàng đã xác nhận đơn. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentResponse {
    private String maVanDon;
    private String trangThaiVanChuyen;
    private String tenTrangThai;
    private Integer soLanGiaoThatBai;
    private String ghiChuGiaoHang;
    private LocalDateTime ngayCapNhat;

    // Lịch sử cập nhật theo thứ tự thời gian tăng dần
    private List<ShipmentEventResponse> lichSu;
}
