package com.oldbook.service.order;

import com.oldbook.constant.order.OrderStatus;
import com.oldbook.constant.shipping.ShipmentStatus;
import com.oldbook.dto.order.CancelRequestResponse;
import com.oldbook.dto.order.OrderItemResponse;
import com.oldbook.dto.order.ShipmentEventResponse;
import com.oldbook.dto.order.ShipmentResponse;
import com.oldbook.dto.order.StoreOrderResponse;
import com.oldbook.entity.order.OrderCancellation;
import com.oldbook.entity.order.StoreOrder;
import com.oldbook.entity.shipping.Shipment;
import com.oldbook.entity.shipping.ShipmentHistory;
import com.oldbook.entity.shipping.ShippingCarrier;
import com.oldbook.entity.system.Review;
import com.oldbook.repository.order.OrderCancellationRepository;
import com.oldbook.repository.order.OrderItemRepository;
import com.oldbook.repository.shipping.ShipmentHistoryRepository;
import com.oldbook.repository.shipping.ShipmentRepository;
import com.oldbook.repository.system.ReviewRepository;
import com.oldbook.service.catalog.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OrderMapper {

    private final OrderItemRepository orderItemRepository;
    private final ShipmentRepository shipmentRepository;
    private final ShipmentHistoryRepository shipmentHistoryRepository;
    private final ReviewRepository reviewRepository;
    private final OrderCancellationRepository orderCancellationRepository;

    public List<StoreOrderResponse> toStoreOrderResponses(List<StoreOrder> stores) {
        if (stores.isEmpty()) {
            return List.of();
        }

        List<Integer> ids = stores.stream().map(StoreOrder::getMaDHCH).toList();

        Map<Integer, Shipment> shipments = shipmentRepository
                .findAllByDonHangCuaHang_MaDHCHIn(ids).stream()
                .collect(Collectors.toMap(
                        v -> v.getDonHangCuaHang().getMaDHCH(),
                        Function.identity(),
                        (a, b) -> a
                ));

        Map<Integer, List<ShipmentHistory>> history = loadHistory(shipments.values());
        Map<Integer, Review> reviews = loadReviews(ids);
        Map<Integer, OrderCancellation> cancelRequests = loadCancelRequests(ids);

        return stores.stream()
                .map(store -> toStoreOrderResponse(store, shipments.get(store.getMaDHCH()), history, reviews,
                        cancelRequests, false))
                .toList();
    }

    /** Dành cho chủ cửa hàng: kèm hoa hồng và doanh thu. Phản hồi cho người mua không có hai khoản này. */
    public StoreOrderResponse toStoreOrderResponse(StoreOrder store, Shipment shipment) {
        return toStoreOrderResponse(
                store, shipment, shipment == null ? Map.of() : loadHistory(List.of(shipment)),
                loadReviews(List.of(store.getMaDHCH())), loadCancelRequests(List.of(store.getMaDHCH())), true);
    }

    private Map<Integer, OrderCancellation> loadCancelRequests(List<Integer> storeOrderIds) {
        return orderCancellationRepository.findAllByDonHangCuaHang_MaDHCHIn(storeOrderIds).stream()
                .collect(Collectors.toMap(
                        r -> r.getDonHangCuaHang().getMaDHCH(),
                        Function.identity(),
                        (a, b) -> a.getMaYeuCau() >= b.getMaYeuCau() ? a : b
                ));
    }

    private Map<Integer, Review> loadReviews(List<Integer> storeOrderIds) {
        return reviewRepository.findAllByChiTietDonHang_DonHangCuaHang_MaDHCHIn(storeOrderIds)
                .stream()
                .collect(Collectors.toMap(
                        review -> review.getChiTietDonHang().getMaCTDH(),
                        Function.identity()
                ));
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

    private StoreOrderResponse toStoreOrderResponse(
            StoreOrder store, Shipment shipment, Map<Integer, List<ShipmentHistory>> history,
            Map<Integer, Review> reviews,
            Map<Integer, OrderCancellation> cancelRequests,
            boolean includeFinance
    ) {
        List<OrderItemResponse> items = orderItemRepository
                .findAllByDonHangCuaHang_MaDHCHOrderByMaCTDHAsc(store.getMaDHCH())
                .stream()
                .map(ct -> OrderItemResponse.builder()
                        .maCTDH(ct.getMaCTDH())
                        .maSach(ct.getSach().getMaSach())
                        .tenSach(ct.getSach().getTenSach())
                        .hinhAnhUrl(ct.getSach().getHinhAnhUrl())
                        .soLuong(ct.getSoLuong())
                        .donGia(ct.getDonGia())
                        .thanhTien(ct.getThanhTien())
                        .coTheDanhGia(!includeFinance
                                && OrderStatus.DA_GIAO.name().equals(store.getTrangThaiDonHang())
                                && !reviews.containsKey(ct.getMaCTDH()))
                        .danhGia(reviews.containsKey(ct.getMaCTDH())
                                ? ReviewService.toResponse(reviews.get(ct.getMaCTDH())) : null)
                        .build())
                .toList();

        ShippingCarrier carrier = store.getDonViVanChuyen();

        return StoreOrderResponse.builder()
                .maDHCH(store.getMaDHCH())
                .maCH(store.getCuaHang().getMaCH())
                .tenCuaHang(store.getCuaHang().getTenCuaHang())
                .tienHangCH(store.getTienHangCH())
                .phiShipCH(store.getPhiShipCH())
                .giamGiaSanPhanBo(store.getGiamGiaSanPhanBo())
                .giamGiaShop(store.getGiamGiaShop())
                .codeVoucherShop(store.getKhuyenMaiShop() == null ? null : store.getKhuyenMaiShop().getCode())
                .tongTienShopOrder(store.getTongTienShopOrder())
                .trangThaiDonHang(store.getTrangThaiDonHang())
                .tenTrangThaiDonHang(orderStatusDisplayName(store.getTrangThaiDonHang()))
                .trangThaiThanhToanShop(store.getTrangThaiThanhToanShop())
                .ghiChu(store.getGhiChu())
                .maDVVC(carrier == null ? null : carrier.getMaDVVC())
                .tenDonViVanChuyen(carrier == null ? null : carrier.getTenDonVi())
                .ngayTao(store.getDonHang() == null ? null : store.getDonHang().getNgayTao())
                .ngayXacNhan(store.getNgayXacNhan())
                .ngayGiao(store.getNgayGiao())
                .ngayHoanTra(store.getNgayHoanTra())
                .ngayHuy(store.getNgayHuy())
                .huyBoi(store.getHuyBoi())
                .lyDoHuy(store.getLyDoHuy())
                .yeuCauHuy(toCancelRequestResponse(cancelRequests.get(store.getMaDHCH())))
                .commissionTien(includeFinance && store.getNgayGiao() != null ? store.getCommissionTien() : null)
                .doanhThuShop(includeFinance && store.getNgayGiao() != null ? store.getDoanhThuShop() : null)
                .vanChuyen(shipment == null ? null
                        : toShipmentResponse(shipment, history.getOrDefault(shipment.getMaVanChuyen(), List.of())))
                .items(items)
                .build();
    }

    private static CancelRequestResponse toCancelRequestResponse(OrderCancellation request) {
        if (request == null) {
            return null;
        }
        return CancelRequestResponse.builder()
                .maYeuCau(request.getMaYeuCau())
                .lyDo(request.getLyDo())
                .lyDoPhanHoi(request.getLyDoPhanHoi())
                .trangThaiYeuCau(request.getTrangThaiYeuCau())
                .ngayGui(request.getNgayGui())
                .ngayXuLy(request.getNgayXuLy())
                .build();
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
