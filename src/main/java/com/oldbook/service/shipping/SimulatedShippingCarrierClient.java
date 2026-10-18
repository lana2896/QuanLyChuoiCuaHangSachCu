package com.oldbook.service.shipping;

import com.oldbook.constant.shipping.ShipmentStatus;
import com.oldbook.entity.shipping.Shipment;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Đơn vị vận chuyển giả lập cho môi trường dev/demo ({@code shipping.sync.provider=simulator}).
 * Mỗi lần tra cứu, vận đơn tiến thêm một bước theo luồng giao thành công.
 * Không dùng cùng cổng đối tác /shipping/{maCode}: giả lập sẽ tự đẩy trạng thái, tranh với thao tác của hãng.
 * Muốn hãng tự cập nhật trên cổng thì đặt {@code shipping.sync.provider=portal}.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "shipping.sync.provider", havingValue = "simulator", matchIfMissing = true)
public class SimulatedShippingCarrierClient implements ShippingCarrierClient {

    private final TrackingCodeGenerator codeGenerator;

    @Override
    public CarrierShipment createShipment(CarrierShipmentRequest request) {
        return new CarrierShipment(codeGenerator.generate(request.maCode(), request.maDHCH()));
    }

    @Override
    public Optional<CarrierTracking> track(Shipment shipment) {
        ShipmentStatus current = ShipmentStatus.fromName(shipment.getTrangThaiVanChuyen());
        if (current == null || current.isFinal()) {
            return Optional.empty();
        }

        String raw = switch (current) {
            case CHO_LAY_HANG -> "picking";
            case DANG_LAY_HANG -> "picked";
            case DA_LAY_HANG -> "delivering";
            case DANG_GIAO -> "delivered";
            case GIAO_THAT_BAI -> "returned";
            default -> null;
        };

        return raw == null
                ? Optional.empty()
                : Optional.of(new CarrierTracking(raw, "Cập nhật từ đơn vị vận chuyển giả lập", LocalDateTime.now()));
    }
}
