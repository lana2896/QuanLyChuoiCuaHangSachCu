package com.oldbook.service.shipping;

import com.oldbook.config.shipping.ShippingProperties;
import com.oldbook.constant.shipping.ShipmentStatus;
import com.oldbook.entity.shipping.Shipment;
import com.oldbook.exception.common.BusinessException;
import com.oldbook.repository.shipping.ShipmentRepository;
import com.oldbook.service.shipping.ShipmentSyncService.CarrierUpdate;
import com.oldbook.service.shipping.ShipmentSyncService.SyncResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Chủ động kéo trạng thái từ API của đơn vị vận chuyển (định kỳ hoặc theo yêu cầu).
 * Cố ý không dùng transaction: cuộc gọi mạng không giữ khóa CSDL; phần ghi nằm trong
 * {@link ShipmentSyncService#applyCarrierUpdate}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShipmentTrackingService {

    private static final List<String> FINAL_STATUSES = Arrays.stream(ShipmentStatus.values())
            .filter(ShipmentStatus::isFinal)
            .map(Enum::name)
            .toList();

    private final ShipmentRepository shipmentRepository;
    private final ShippingCarrierClient carrierClient;
    private final ShipmentSyncService syncService;
    private final ShippingProperties properties;

    /** Đồng bộ một đơn hàng con theo yêu cầu. */
    public SyncResult pullLatest(Integer maDHCH) {
        Shipment vanChuyen = shipmentRepository.findWithCarrierByMaDHCH(maDHCH)
                .orElseThrow(() -> new BusinessException(
                        "Đơn hàng chưa được chuyển cho đơn vị vận chuyển"));
        return pull(vanChuyen);
    }

    /** Đồng bộ mọi vận đơn chưa kết thúc. Trả về số vận đơn có thay đổi. */
    public int pollActiveShipments() {
        int batchSize = Math.max(1, properties.getSync().getBatchSize());
        int changed = 0;
        int afterId = 0;

        while (true) {
            List<Shipment> batch = shipmentRepository
                    .findActiveAfter(FINAL_STATUSES, afterId, PageRequest.of(0, batchSize));
            if (batch.isEmpty()) {
                return changed;
            }
            for (Shipment vanChuyen : batch) {
                afterId = vanChuyen.getMaVanChuyen();
                try {
                    if (pull(vanChuyen) == SyncResult.APPLIED) {
                        changed++;
                    }
                } catch (RuntimeException ex) {
                    // Một vận đơn lỗi không được chặn các vận đơn còn lại.
                    log.warn("Không đồng bộ được vận đơn {}: {}", vanChuyen.getMaVanDon(), ex.getMessage());
                }
            }
        }
    }

    private SyncResult pull(Shipment vanChuyen) {
        ShipmentStatus current = ShipmentStatus.fromName(vanChuyen.getTrangThaiVanChuyen());
        if (current == null || current.isFinal()) {
            return SyncResult.NO_CHANGE;
        }

        Optional<ShippingCarrierClient.CarrierTracking> tracking = carrierClient.track(vanChuyen);
        if (tracking.isEmpty()) {
            return SyncResult.NO_CHANGE;
        }

        ShippingCarrierClient.CarrierTracking t = tracking.get();
        return syncService.applyCarrierUpdate(
                vanChuyen.getDonViVanChuyen().getMaCode(),
                new CarrierUpdate(vanChuyen.getMaVanDon(), t.trangThaiGoc(), t.ghiChu(), t.thoiGian(),
                        ShipmentSyncService.SOURCE_POLLING));
    }
}
