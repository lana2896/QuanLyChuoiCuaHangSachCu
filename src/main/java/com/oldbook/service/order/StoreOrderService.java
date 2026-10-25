package com.oldbook.service.order;

import com.oldbook.constant.auth.Role;
import com.oldbook.constant.order.CancellationStatus;
import com.oldbook.constant.order.OrderStatus;
import com.oldbook.constant.shipping.ShipmentStatus;
import com.oldbook.dto.order.CancelOrderRequest;
import com.oldbook.dto.order.OrderItemResponse;
import com.oldbook.dto.order.SellerOrderResponse;
import com.oldbook.dto.order.StoreOrderProductOption;
import com.oldbook.entity.catalog.Store;
import com.oldbook.entity.order.Order;
import com.oldbook.entity.order.OrderCancellation;
import com.oldbook.entity.order.StoreOrder;
import com.oldbook.entity.shipping.ShipmentHistory;
import com.oldbook.entity.shipping.Shipment;
import com.oldbook.entity.shipping.ShippingCarrier;
import com.oldbook.exception.common.BusinessException;
import com.oldbook.repository.order.OrderCancellationRepository;
import com.oldbook.repository.order.OrderItemRepository;
import com.oldbook.repository.order.StoreOrderRepository;
import com.oldbook.repository.shipping.ShipmentHistoryRepository;
import com.oldbook.repository.shipping.ShipmentRepository;
import com.oldbook.repository.store.StoreRepository;
import com.oldbook.service.shipping.ShippingCarrierClient;
import com.oldbook.service.shipping.ShippingCarrierClient.CarrierShipment;
import com.oldbook.service.shipping.ShippingCarrierClient.CarrierShipmentRequest;
import com.oldbook.service.notification.BusinessNotificationService;
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
    private final BusinessNotificationService businessNotificationService;
    private final OrderItemRepository orderItemRepository;
    private final OrderCancellationRepository orderCancellationRepository;
    private final OrderSettlementService settlementService;

    private static final String NOT_REVIEWED = "CHUA_DANH_GIA";

    @Transactional(readOnly = true)
    public List<SellerOrderResponse> getMyStoreOrders(Integer maND, String trangThai) {
        return getMyStoreOrders(maND, trangThai, null, null, null);
    }

    @Transactional(readOnly = true)
    public List<SellerOrderResponse> getMyStoreOrders(
            Integer maND, String trangThai, String maDonHang, Integer maSach, String danhGia
    ) {
        Store store = requireStore(maND);

        String keyword = normalizeOrderCode(maDonHang);
        boolean notReviewed = danhGia != null && NOT_REVIEWED.equalsIgnoreCase(danhGia.trim());
        Integer soSao = notReviewed ? null : parseStars(danhGia);

        List<StoreOrder> orders = storeOrderRepository
                .findAllByCuaHang_MaCHOrderByMaDHCHDesc(store.getMaCH())
                .stream()
                .filter(o -> trangThai == null || trangThai.isBlank()
                        || trangThai.trim().equalsIgnoreCase(o.getTrangThaiDonHang()))
                .filter(o -> keyword.isEmpty() || String.valueOf(o.getMaDHCH()).contains(keyword))
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
                .filter(r -> matchesItemFilters(r, maSach, soSao, notReviewed))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StoreOrderProductOption> getMyStoreOrderProducts(Integer maND) {
        Store store = requireStore(maND);
        return orderItemRepository.findDistinctProductsByStore(store.getMaCH());
    }

    private static String normalizeOrderCode(String value) {
        if (value == null) {
            return "";
        }
        String code = value.trim();
        if (code.startsWith("#")) {
            code = code.substring(1).trim();
        }
        return code;
    }

    private static Integer parseStars(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            int stars = Integer.parseInt(value.trim());
            if (stars >= 1 && stars <= 5) {
                return stars;
            }
        } catch (NumberFormatException ignored) {
        }
        throw new BusinessException("Bộ lọc đánh giá không hợp lệ");
    }

    private static boolean matchesItemFilters(
            SellerOrderResponse response, Integer maSach, Integer soSao, boolean notReviewed
    ) {
        if (maSach == null && soSao == null && !notReviewed) {
            return true;
        }
        List<OrderItemResponse> items = response.getStoreOrder().getItems() == null
                ? List.of() : response.getStoreOrder().getItems();
        List<OrderItemResponse> scope = maSach == null ? items : items.stream()
                .filter(i -> maSach.equals(i.getMaSach()))
                .toList();
        if (scope.isEmpty()) {
            return false;
        }
        if (soSao != null) {
            return scope.stream().anyMatch(i -> i.getDanhGia() != null && soSao.equals(i.getDanhGia().soSao()));
        }
        if (notReviewed) {
            return OrderStatus.DA_GIAO.name().equals(response.getStoreOrder().getTrangThaiDonHang())
                    && scope.stream().anyMatch(i -> i.getDanhGia() == null);
        }
        return true;
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
        businessNotificationService.orderConfirmed(order);

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
                .ghiChu("Chủ cửa hàng đã xác nhận, chờ " + carrier.getTenDonVi() + " tiếp nhận")
                .nguon(ShipmentHistory.SOURCE_SYSTEM)
                .build());

        return toSellerResponse(order, shipment);
    }

    @Transactional
    public SellerOrderResponse cancelOrder(Integer maND, Integer maDHCH, String lyDo) {
        String reason = CancelOrderRequest.normalize(lyDo);
        StoreOrder order = loadOwnedOrderForUpdate(maND, maDHCH);
        String status = order.getTrangThaiDonHang();

        if (OrderStatus.CHO_XU_LY.name().equals(status)) {
            settlementService.settleCancelled(order, Role.CHU_CUA_HANG.name(), reason);
            return toSellerResponse(order, null);
        }

        Shipment shipment = shipmentRepository.findByDonHangCuaHang_MaDHCH(maDHCH).orElse(null);
        boolean carrierNotAccepted = shipment == null
                || ShipmentStatus.CHO_LAY_HANG.name().equals(shipment.getTrangThaiVanChuyen());

        if (!OrderStatus.DA_XAC_NHAN.name().equals(status) || !carrierNotAccepted) {
            throw new BusinessException("Đơn vị vận chuyển đã tiếp nhận đơn, không thể hủy");
        }

        settlementService.settleCancelled(order, Role.CHU_CUA_HANG.name(), reason);
        markShipmentCancelled(shipment, "Đơn bị cửa hàng hủy");

        orderCancellationRepository.findFirstByDonHangCuaHang_MaDHCHAndTrangThaiYeuCau(
                        maDHCH, CancellationStatus.CHO_DUYET.name())
                .ifPresent(request -> resolveRequest(request, CancellationStatus.DA_DUYET, null));

        return toSellerResponse(order, shipment);
    }

    @Transactional
    public SellerOrderResponse approveCancelRequest(Integer maND, Integer maDHCH) {
        StoreOrder order = loadOwnedOrderForUpdate(maND, maDHCH);
        OrderCancellation request = requirePendingCancelRequest(maDHCH);

        Shipment shipment = shipmentRepository.findByDonHangCuaHang_MaDHCH(maDHCH).orElse(null);
        String shipStatus = shipment == null ? null : shipment.getTrangThaiVanChuyen();
        boolean notPickedUp = shipStatus == null
                || ShipmentStatus.CHO_LAY_HANG.name().equals(shipStatus)
                || ShipmentStatus.DANG_LAY_HANG.name().equals(shipStatus);
        boolean orderCancellable = OrderStatus.DA_XAC_NHAN.name().equals(order.getTrangThaiDonHang())
                || OrderStatus.DANG_LAY_HANG.name().equals(order.getTrangThaiDonHang());
        if (!orderCancellable || !notPickedUp) {
            throw new BusinessException("Đơn vị vận chuyển đã lấy hàng, không thể hủy. Vui lòng từ chối yêu cầu hủy");
        }

        settlementService.settleCancelled(order, Role.KHACH_HANG.name(), request.getLyDo());
        markShipmentCancelled(shipment, "Đơn bị hủy theo yêu cầu của khách");
        resolveRequest(request, CancellationStatus.DA_DUYET, null);

        return toSellerResponse(order, shipment);
    }

    @Transactional
    public SellerOrderResponse rejectCancelRequest(Integer maND, Integer maDHCH, String lyDo) {
        String reason = CancelOrderRequest.normalize(lyDo);
        StoreOrder order = loadOwnedOrderForUpdate(maND, maDHCH);
        OrderCancellation request = requirePendingCancelRequest(maDHCH);

        resolveRequest(request, CancellationStatus.TU_CHOI, reason);
        return toSellerResponse(order, shipmentRepository.findByDonHangCuaHang_MaDHCH(maDHCH).orElse(null));
    }

    private OrderCancellation requirePendingCancelRequest(Integer maDHCH) {
        return orderCancellationRepository.findFirstByDonHangCuaHang_MaDHCHAndTrangThaiYeuCau(
                        maDHCH, CancellationStatus.CHO_DUYET.name())
                .orElseThrow(() -> new BusinessException("Đơn hàng không có yêu cầu hủy đang chờ xử lý"));
    }

    private void resolveRequest(OrderCancellation request, CancellationStatus status, String phanHoi) {
        request.setTrangThaiYeuCau(status.name());
        request.setLyDoPhanHoi(phanHoi);
        request.setNgayXuLy(LocalDateTime.now());
        orderCancellationRepository.save(request);
    }

    private void markShipmentCancelled(Shipment shipment, String note) {
        if (shipment == null) {
            return;
        }
        shipment.setTrangThaiVanChuyen(ShipmentStatus.DA_HUY.name());
        shipment.setGhiChuGiaoHang(note);
        shipmentRepository.save(shipment);

        shipmentHistoryRepository.save(ShipmentHistory.builder()
                .vanChuyen(shipment)
                .trangThai(ShipmentStatus.DA_HUY.name())
                .ghiChu(note)
                .nguon(ShipmentHistory.SOURCE_SYSTEM)
                .build());
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
