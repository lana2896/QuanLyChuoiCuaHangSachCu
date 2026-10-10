package com.oldbook.dto.order;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {
    private Integer maDH;
    private Integer maNDNguoiMua;
    private BigDecimal tongTienHang;
    private BigDecimal tongPhiShip;
    private BigDecimal tongGiamGia;
    private BigDecimal tongThanhToan;
    private String phuongThucTT;
    private String trangThaiThanhToan;
    private Integer maDiaChi;
    private String tenNguoiNhan;
    private String soDienThoaiNhan;
    private String tinhThanhGiao;
    private String quanHuyenGiao;
    private String phuongXaGiao;
    private String diaChiGiaoChiTiet;
    private LocalDateTime ngayTao;
    private List<StoreOrderResponse> storeOrders;
}
