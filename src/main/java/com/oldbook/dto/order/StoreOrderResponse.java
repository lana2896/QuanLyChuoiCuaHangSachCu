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
public class StoreOrderResponse {
    private Integer maDHCH;
    private Integer maCH;
    private String tenCuaHang;
    private BigDecimal tienHangCH;
    private BigDecimal phiShipCH;
    private BigDecimal tongTienShopOrder;
    private String trangThaiDonHang;
    private String tenTrangThaiDonHang;
    private String trangThaiThanhToanShop;

    // Ghi chú riêng của khách cho cửa hàng này
    private String ghiChu;

    // Đơn vị vận chuyển khách đã chọn cho đơn hàng con này
    private Integer maDVVC;
    private String tenDonViVanChuyen;

    private LocalDateTime ngayXacNhan;

    // Chỉ có khi đã giao thành công / đã hoàn trả
    private LocalDateTime ngayGiao;
    private LocalDateTime ngayHoanTra;

    // Chỉ có khi đã giao thành công
    private BigDecimal commissionTien;
    private BigDecimal doanhThuShop;

    // null cho tới khi cửa hàng xác nhận đơn
    private ShipmentResponse vanChuyen;

    private List<OrderItemResponse> items;
}
