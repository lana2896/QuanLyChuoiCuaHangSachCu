package com.oldbook.service.order;

import com.oldbook.constant.order.OrderStatus;
import com.oldbook.constant.shipping.ShipmentStatus;
import com.oldbook.dto.order.SellerOrderResponse;
import com.oldbook.entity.catalog.Store;
import com.oldbook.entity.order.Order;
import com.oldbook.entity.order.StoreOrder;
import com.oldbook.entity.shipping.ShipmentHistory;
import com.oldbook.entity.shipping.Shipment;
import com.oldbook.entity.shipping.ShippingCarrier;
import com.oldbook.exception.common.BusinessException;
import com.oldbook.repository.order.StoreOrderRepository;
import com.oldbook.repository.shipping.ShipmentHistoryRepository;
import com.oldbook.repository.shipping.ShipmentRepository;
import com.oldbook.repository.store.StoreRepository;
import com.oldbook.service.shipping.ShippingCarrierClient;
import com.oldbook.service.shipping.ShippingCarrierClient.CarrierShipment;
import com.oldbook.service.shipping.ShippingCarrierClient.CarrierShipmentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoreOrderService {

    private final StoreRepository storeRepository;
    private final StoreOrderRepository storeOrderRepository;
    private final ShipmentRepository shipmentRepository;
    private final ShipmentHistoryRepository shipmentHistoryRepository;
    private final ShippingCarrierClient carrierClient;
    private final OrderMapper orderMapper;

    @Transactional(readOnly = true)
    public List<SellerOrderResponse> getMyStoreOrders(Integer maND, String trangThai) {
        Store store = requireStore(maND);

        List<StoreOrder> orders = storeOrderRepository
                .findAllByCuaHang_MaCHOrderByMaDHCHDesc(store.getMaCH())
                .stream()
                .filter(o -> trangThai == null || trangThai.isBlank()
                        || trangThai.trim().equalsIgnoreCase(o.getTrangThaiDonHang()))
                .toList();

        if (orders.isEmpty()) {
            return List.of();
        }

        Map<Integer, Shipment> shipments = shipmentRepository
                .findAllByDonHangCuaHang_MaDHCHIn(
                        orders.stream().map(StoreOrder::getMaDHCH).toList()
                ).stream()
                .collect(Collectors.toMap(
                        v -> v.getDonHangCuaHang().getMaDHCH(),
                        Function.identity(),
                        (a, b) -> a
                ));

        return orders.stream()
                .map(o -> toSellerResponse(o, shipments.get(o.getMaDHCH())))
                .toList();
    }

    /**
     * Chủ cửa hàng xác nhận đơn. Sau bước này đơn được chuyển cho đơn vị vận chuyển tiếp nhận;
     * mọi trạng thái giao hàng về sau do đơn vị vận chuyển cập nhật (đồng bộ qua API),
     * chủ cửa hàng không tự đổi được.
     */
    @Transactional
    public SellerOrderResponse confirmOrder(Integer maND, Integer maDHCH) {
        StoreOrder order = loadOwnedOrderForUpdate(maND, maDHCH);

        if (!OrderStatus.CHO_XU_LY.name().equals(order.getTrangThaiDonHang())) {
            throw new BusinessException("Chỉ có thể xác nhận đơn hàng đang chờ xử lý");
        }

        ShippingCarrier carrier = order.getDonViVanChuyen();
        if (carrier == null) {
            throw new BusinessException("Đơn hàng chưa có đơn vị vận chuyển");
        }

        if (shipmentRepository.findByDonHangCuaHang_MaDHCH(maDHCH).isPresent()) {
            throw new BusinessException("Đơn hàng đã được xác nhận trước đó");
        }

        Order donHang = order.getDonHang();

        // Bàn giao đơn cho đơn vị vận chuyển; lỗi ở đây làm hủy cả bước xác nhận để cửa hàng thử lại.
        CarrierShipment carrierShipment = carrierClient.createShipment(new CarrierShipmentRequest(
                carrier.getMaCode(),
                order.getMaDHCH(),
                donHang.getTenNguoiNhan(),
                donHang.getSoDienThoaiNhan(),
                formatAddress(donHang),
                order.getTongTienShopOrder(),
                order.getGhiChu()
        ));

        order.setTrangThaiDonHang(OrderStatus.DA_XAC_NHAN.name());
        order.setNgayXacNhan(LocalDateTime.now());
        storeOrderRepository.save(order);

        Shipment shipment = shipmentRepository.save(
                Shipment.builder()
                        .donHangCuaHang(order)
                        .donViVanChuyen(carrier)
                        .maVanDon(carrierShipment.maVanDon())
                        .trangThaiVanChuyen(ShipmentStatus.CHO_LAY_HANG.name())
                        .soLanGiaoThatBai(0)
                        .build()
        );

        shipmentHistoryRepository.save(ShipmentHistory.builder()
                .vanChuyen(shipment)
                .trangThai(ShipmentStatus.CHO_LAY_HANG.name())
                .ghiChu("Cửa hàng đã xác nhận, " + carrier.getTenDonVi() + " đã tiếp nhận đơn")
                .nguon(ShipmentHistory.SOURCE_SYSTEM)
                .build());

        return toSellerResponse(order, shipment);
    }

    private Store requireStore(Integer maND) {
        return storeRepository.findByChuShop_MaND(maND)
                .orElseThrow(() -> new BusinessException("Bạn chưa có cửa hàng"));
    }

    private StoreOrder loadOwnedOrderForUpdate(Integer maND, Integer maDHCH) {
        Store store = requireStore(maND);

        StoreOrder order = storeOrderRepository.findByIdForUpdate(maDHCH)
                .orElseThrow(() -> new BusinessException("Đơn hàng không tồn tại"));

        // Trả cùng một thông báo để không lộ đơn của cửa hàng khác.
        if (order.getCuaHang() == null
                || !store.getMaCH().equals(order.getCuaHang().getMaCH())) {
            throw new BusinessException("Đơn hàng không tồn tại");
        }

        return order;
    }

    private SellerOrderResponse toSellerResponse(StoreOrder order, Shipment shipment) {
        Order donHang = order.getDonHang();

        String diaChi = formatAddress(donHang);

        return SellerOrderResponse.builder()
                .maDH(donHang.getMaDH())
                .ngayTao(donHang.getNgayTao())
                .phuongThucTT(donHang.getPhuongThucTT())
                .tenNguoiNhan(donHang.getTenNguoiNhan())
                .soDienThoaiNhan(donHang.getSoDienThoaiNhan())
                .diaChiGiao(diaChi)
                .storeOrder(orderMapper.toStoreOrderResponse(order, shipment))
                .build();
    }

    private static String formatAddress(Order donHang) {
        return java.util.stream.Stream.of(
                        donHang.getDiaChiGiaoChiTiet(),
                        donHang.getPhuongXaGiao(),
                        donHang.getQuanHuyenGiao(),
                        donHang.getTinhThanhGiao())
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(", "));
    }
}
