package com.oldbook.service.shipping;

import com.oldbook.config.shipping.ShippingProperties;
import com.oldbook.constant.shipping.ShipmentStatus;
import com.oldbook.entity.order.StoreOrder;
import com.oldbook.entity.shipping.ShipmentHistory;
import com.oldbook.entity.shipping.Shipment;
import com.oldbook.exception.common.BusinessException;
import com.oldbook.repository.order.StoreOrderRepository;
import com.oldbook.repository.shipping.ShipmentHistoryRepository;
import com.oldbook.repository.shipping.ShipmentRepository;
import com.oldbook.service.order.OrderSettlementService;
import com.oldbook.service.notification.BusinessNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Điểm duy nhất áp dụng trạng thái giao hàng do đơn vị vận chuyển báo về, từ hai nguồn:
 * <ul>
 *   <li>Tra cứu API của hãng (đồng bộ định kỳ / theo yêu cầu) - {@link #applyCarrierUpdate};</li>
 *   <li>Hãng tự cập nhật trên cổng đối tác /shipping/{maCode} - {@link #applyPartnerUpdate}.</li>
 * </ul>
 * Cả hai dùng chung state machine, ghi lịch sử, tự chuyển hoàn hàng và chốt tiền COD.
 * Xử lý idempotent: kết quả lặp hoặc lạc hậu không làm đơn lùi trạng thái hay chốt tiền hai lần.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShipmentSyncService {

    public static final String SOURCE_POLLING = "POLLING";
    public static final String SOURCE_SYSTEM = "HE_THONG";
    /** Đơn vị vận chuyển cập nhật trên cổng đối tác /shipping/{maCode}. */
    public static final String SOURCE_PARTNER = "DOI_TAC";

    public enum SyncResult {
        APPLIED, NO_CHANGE, IGNORED_UNKNOWN_STATUS, IGNORED_INVALID_TRANSITION, NOT_FOUND
    }

    public record CarrierUpdate(
            String maVanDon, String trangThaiGoc, String ghiChu, LocalDateTime thoiGian, String nguon
    ) {}

    private final ShipmentRepository shipmentRepository;
    private final StoreOrderRepository storeOrderRepository;
    private final ShipmentHistoryRepository shipmentHistoryRepository;
    private final CarrierStatusMapper statusMapper;
    private final OrderSettlementService settlementService;
    private final ShippingProperties properties;
    private final BusinessNotificationService businessNotificationService;

    /**
     * @param expectedMaCode mã đơn vị vận chuyển phải sở hữu vận đơn; null = không kiểm tra
     */
    @Transactional
    public SyncResult applyCarrierUpdate(String expectedMaCode, CarrierUpdate update) {
        if (update.maVanDon() == null || update.maVanDon().isBlank()) {
            return SyncResult.NOT_FOUND;
        }

        Optional<Integer> maDHCH = shipmentRepository.findMaDHCHByMaVanDon(update.maVanDon().trim());
        if (maDHCH.isEmpty()) {
            return SyncResult.NOT_FOUND;
        }

        // Luôn khóa đơn hàng con trước, rồi mới đọc vận đơn, để các luồng cập nhật song song tuần tự hóa.
        StoreOrder order = storeOrderRepository.findByIdForUpdate(maDHCH.get()).orElse(null);
        Shipment vanChuyen = order == null ? null
                : shipmentRepository.findByDonHangCuaHang_MaDHCH(order.getMaDHCH()).orElse(null);
        if (vanChuyen == null) {
            return SyncResult.NOT_FOUND;
        }

        String ownerCode = vanChuyen.getDonViVanChuyen().getMaCode();
        if (expectedMaCode != null && !expectedMaCode.trim().equalsIgnoreCase(ownerCode)) {
            return SyncResult.NOT_FOUND;
        }

        Optional<ShipmentStatus> mapped = statusMapper.map(ownerCode, update.trangThaiGoc());
        if (mapped.isEmpty()) {
            log.warn("Bỏ qua trạng thái không nhận diện '{}' của vận đơn {} ({})",
                    update.trangThaiGoc(), update.maVanDon(), ownerCode);
            return SyncResult.IGNORED_UNKNOWN_STATUS;
        }

        ShipmentStatus current = ShipmentStatus.fromName(vanChuyen.getTrangThaiVanChuyen());
        ShipmentStatus next = mapped.get();

        if (current == next) {
            return SyncResult.NO_CHANGE;
        }
        if (current == null || !current.canMoveTo(next)) {
            log.info("Bỏ qua chuyển trạng thái không hợp lệ {} -> {} của vận đơn {}",
                    current, next, update.maVanDon());
            return SyncResult.IGNORED_INVALID_TRANSITION;
        }

        transition(order, vanChuyen, next, update.trangThaiGoc(), update.ghiChu(), update.thoiGian(), update.nguon());
        return SyncResult.APPLIED;
    }

    /**
     * Đơn vị vận chuyển cập nhật trên cổng đối tác. Khác với đồng bộ API (bỏ qua kết quả lạ/lạc hậu),
     * ở đây người dùng thao tác trực tiếp nên bước chuyển sai sẽ báo lỗi rõ ràng.
     *
     * @param maDHCH đơn hàng con của vận đơn; người gọi đã kiểm tra vận đơn thuộc đúng đơn vị vận chuyển
     */
    @Transactional
    public Shipment applyPartnerUpdate(Integer maDHCH, ShipmentStatus next, String ghiChu) {
        // Khóa đơn hàng con trước, giống applyCarrierUpdate và luồng xác nhận của cửa hàng.
        StoreOrder order = storeOrderRepository.findByIdForUpdate(maDHCH)
                .orElseThrow(() -> new BusinessException("Đơn hàng không tồn tại"));
        Shipment vanChuyen = shipmentRepository.findByDonHangCuaHang_MaDHCH(maDHCH)
                .orElseThrow(() -> new BusinessException("Vận chuyển không tồn tại"));

        ShipmentStatus current = ShipmentStatus.fromName(vanChuyen.getTrangThaiVanChuyen());
        if (current == null) {
            throw new BusinessException("Trạng thái hiện tại của vận đơn không hợp lệ");
        }
        if (current == next) {
            throw new BusinessException("Vận đơn đang ở trạng thái \"" + next.getDisplayName() + "\"");
        }
        if (!current.canMoveTo(next)) {
            throw new BusinessException("Không thể chuyển từ \"" + current.getDisplayName()
                    + "\" sang \"" + next.getDisplayName() + "\"");
        }

        transition(order, vanChuyen, next, null, ghiChu, null, SOURCE_PARTNER);
        return vanChuyen;
    }

    /** Chuyển trạng thái; giao thất bại đủ số lần cho phép thì tự chuyển hoàn hàng. */
    private void transition(
            StoreOrder order, Shipment vanChuyen, ShipmentStatus next,
            String trangThaiGoc, String ghiChu, LocalDateTime thoiGian, String nguon
    ) {
        moveTo(order, vanChuyen, next, trangThaiGoc, ghiChu, thoiGian, nguon);

        if (next == ShipmentStatus.GIAO_THAT_BAI
                && nz(vanChuyen.getSoLanGiaoThatBai()) >= Math.max(1, properties.getMaxDeliveryAttempts())) {
            moveTo(order, vanChuyen, ShipmentStatus.DA_HOAN_TRA, null,
                    "Giao thất bại " + vanChuyen.getSoLanGiaoThatBai() + " lần, chuyển hoàn hàng về cửa hàng",
                    null, SOURCE_SYSTEM);
        }
    }

    private void moveTo(
            StoreOrder order, Shipment vanChuyen, ShipmentStatus next,
            String trangThaiGoc, String ghiChu, LocalDateTime thoiGian, String nguon
    ) {
        String note = trim(ghiChu, 1000);

        vanChuyen.setTrangThaiVanChuyen(next.name());
        if (note != null) {
            vanChuyen.setGhiChuGiaoHang(note);
        }
        if (next == ShipmentStatus.GIAO_THAT_BAI) {
            vanChuyen.setSoLanGiaoThatBai(nz(vanChuyen.getSoLanGiaoThatBai()) + 1);
        }
        shipmentRepository.save(vanChuyen);

        order.setTrangThaiDonHang(next.toOrderStatus().name());
        storeOrderRepository.save(order);
        businessNotificationService.orderShippingStatusChanged(order, next.getDisplayName());

        shipmentHistoryRepository.save(ShipmentHistory.builder()
                .vanChuyen(vanChuyen)
                .trangThai(next.name())
                .trangThaiGoc(trim(trangThaiGoc, 100))
                .ghiChu(note)
                .nguon(nguon == null ? SOURCE_SYSTEM : nguon)
                .thoiGian(thoiGian)
                .build());

        if (next == ShipmentStatus.DA_GIAO) {
            settlementService.settleDelivered(order, vanChuyen.getMaVanDon());
        } else if (next == ShipmentStatus.DA_HOAN_TRA) {
            settlementService.settleReturned(order);
        }
    }

    private static String trim(String value, int max) {
        if (value == null) {
            return null;
        }
        String t = value.trim();
        if (t.isEmpty()) {
            return null;
        }
        return t.length() > max ? t.substring(0, max) : t;
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }
}
