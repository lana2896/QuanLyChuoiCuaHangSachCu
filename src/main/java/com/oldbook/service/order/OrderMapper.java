package com.oldbook.service.order;

import com.oldbook.constant.order.OrderStatus;
import com.oldbook.constant.shipping.ShipmentStatus;
import com.oldbook.dto.order.OrderItemResponse;
import com.oldbook.dto.order.ShipmentEventResponse;
import com.oldbook.dto.order.ShipmentResponse;
import com.oldbook.dto.order.StoreOrderResponse;
import com.oldbook.entity.order.StoreOrder;
import com.oldbook.entity.shipping.Shipment;
import com.oldbook.entity.shipping.ShipmentHistory;
import com.oldbook.entity.shipping.ShippingCarrier;
import com.oldbook.repository.order.OrderItemRepository;
import com.oldbook.repository.shipping.ShipmentHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OrderMapper {

    private final OrderItemRepository orderItemRepository;
    private final ShipmentHistoryRepository shipmentHistoryRepository;

    /** Dành cho chủ cửa hàng: kèm hoa hồng và doanh thu. */
    public StoreOrderResponse toStoreOrderResponse(StoreOrder store, Shipment shipment) {
        Map<Integer, List<ShipmentHistory>> history =
                shipment == null ? Map.of() : loadHistory(List.of(shipment));

        List<OrderItemResponse> items = orderItemRepository
                .findAllByDonHangCuaHang_MaDHCHOrderByMaCTDHAsc(store.getMaDHCH())
                .stream()
                .map(ct -> OrderItemResponse.builder()
                        .maCTDH(ct.getMaCTDH())
                        .maSach(ct.getSach().getMaSach())
                        .tenSach(ct.getSach().getTenSach())
                        .soLuong(ct.getSoLuong())
                        .donGia(ct.getDonGia())
                        .thanhTien(ct.getThanhTien())
                        .build())
                .toList();

        ShippingCarrier carrier = store.getDonViVanChuyen();

        return StoreOrderResponse.builder()
                .maDHCH(store.getMaDHCH())
                .maCH(store.getCuaHang().getMaCH())
                .tenCuaHang(store.getCuaHang().getTenCuaHang())
                .tienHangCH(store.getTienHangCH())
                .phiShipCH(store.getPhiShipCH())
                .tongTienShopOrder(store.getTongTienShopOrder())
                .trangThaiDonHang(store.getTrangThaiDonHang())
                .tenTrangThaiDonHang(orderStatusDisplayName(store.getTrangThaiDonHang()))
                .trangThaiThanhToanShop(store.getTrangThaiThanhToanShop())
                .ghiChu(store.getGhiChu())
                .maDVVC(carrier == null ? null : carrier.getMaDVVC())
                .tenDonViVanChuyen(carrier == null ? null : carrier.getTenDonVi())
                .ngayXacNhan(store.getNgayXacNhan())
                .ngayGiao(store.getNgayGiao())
                .ngayHoanTra(store.getNgayHoanTra())
                .commissionTien(store.getNgayGiao() != null ? store.getCommissionTien() : null)
                .doanhThuShop(store.getNgayGiao() != null ? store.getDoanhThuShop() : null)
                .vanChuyen(shipment == null ? null
                        : toShipmentResponse(shipment, history.getOrDefault(shipment.getMaVanChuyen(), List.of())))
                .items(items)
                .build();
    }

    private Map<Integer, List<ShipmentHistory>> loadHistory(Collection<Shipment> shipments) {
        if (shipments.isEmpty()) {
            return Map.of();
        }
        return shipmentHistoryRepository
                .findAllByVanChuyen_MaVanChuyenInOrderByThoiGianAscMaLichSuAsc(
                        shipments.stream().map(Shipment::getMaVanChuyen).toList())
                .stream()
                .collect(Collectors.groupingBy(h -> h.getVanChuyen().getMaVanChuyen()));
    }

    private static String orderStatusDisplayName(String value) {
        OrderStatus st = OrderStatus.fromName(value);
        return st == null ? value : st.getDisplayName();
    }

    private ShipmentResponse toShipmentResponse(Shipment vc, List<ShipmentHistory> history) {
        ShipmentStatus st = ShipmentStatus.fromName(vc.getTrangThaiVanChuyen());
        return ShipmentResponse.builder()
                .maVanDon(vc.getMaVanDon())
                .trangThaiVanChuyen(vc.getTrangThaiVanChuyen())
                .tenTrangThai(st == null ? vc.getTrangThaiVanChuyen() : st.getDisplayName())
                .lichSu(history.stream().map(h -> {
                    ShipmentStatus hs = ShipmentStatus.fromName(h.getTrangThai());
                    return ShipmentEventResponse.builder()
                            .trangThai(h.getTrangThai())
                            .tenTrangThai(hs == null ? h.getTrangThai() : hs.getDisplayName())
                            .ghiChu(h.getGhiChu())
                            .thoiGian(h.getThoiGian())
                            .build();
                }).toList())
                .soLanGiaoThatBai(vc.getSoLanGiaoThatBai())
                .ghiChuGiaoHang(vc.getGhiChuGiaoHang())
                .ngayCapNhat(vc.getNgayCapNhat())
                .build();
    }
}
