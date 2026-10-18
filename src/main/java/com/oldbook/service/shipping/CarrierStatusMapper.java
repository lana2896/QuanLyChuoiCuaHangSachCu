package com.oldbook.service.shipping;

import com.oldbook.config.shipping.ShippingProperties;
import com.oldbook.constant.shipping.ShipmentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static com.oldbook.constant.shipping.ShipmentStatus.*;

/**
 * Chuyển mã trạng thái gốc của đơn vị vận chuyển sang {@link ShipmentStatus}.
 * Bảng mặc định gồm tên trạng thái nội bộ và các mã phổ biến (theo kiểu GHN);
 * từng đơn vị có thể ghi đè bằng {@code shipping.carriers.<MA>.status-map.<ma-goc>=<TRANG_THAI>}.
 */
@Component
@RequiredArgsConstructor
public class CarrierStatusMapper {

    private static final Map<String, ShipmentStatus> DEFAULTS = Map.ofEntries(
            // đã tiếp nhận, chờ lấy hàng
            Map.entry("cho_lay_hang", CHO_LAY_HANG),
            Map.entry("ready_to_pick", CHO_LAY_HANG),
            Map.entry("created", CHO_LAY_HANG),
            Map.entry("waiting_pickup", CHO_LAY_HANG),
            // đang lấy hàng
            Map.entry("dang_lay_hang", DANG_LAY_HANG),
            Map.entry("picking", DANG_LAY_HANG),
            Map.entry("money_collect_picking", DANG_LAY_HANG),
            Map.entry("pickup_in_progress", DANG_LAY_HANG),
            // đã lấy hàng (đang trung chuyển giữa các kho)
            Map.entry("da_lay_hang", DA_LAY_HANG),
            Map.entry("picked", DA_LAY_HANG),
            Map.entry("picked_up", DA_LAY_HANG),
            Map.entry("storing", DA_LAY_HANG),
            Map.entry("transporting", DA_LAY_HANG),
            Map.entry("sorting", DA_LAY_HANG),
            Map.entry("in_transit", DA_LAY_HANG),
            // đang giao
            Map.entry("dang_giao", DANG_GIAO),
            Map.entry("delivering", DANG_GIAO),
            Map.entry("money_collect_delivering", DANG_GIAO),
            Map.entry("out_for_delivery", DANG_GIAO),
            // đã giao
            Map.entry("da_giao", DA_GIAO),
            Map.entry("delivered", DA_GIAO),
            Map.entry("delivered_success", DA_GIAO),
            // giao thất bại
            Map.entry("giao_that_bai", GIAO_THAT_BAI),
            Map.entry("delivery_fail", GIAO_THAT_BAI),
            Map.entry("delivery_failed", GIAO_THAT_BAI),
            // hoàn hàng
            Map.entry("da_hoan_tra", DA_HOAN_TRA),
            Map.entry("waiting_to_return", DA_HOAN_TRA),
            Map.entry("return", DA_HOAN_TRA),
            Map.entry("returning", DA_HOAN_TRA),
            Map.entry("return_transporting", DA_HOAN_TRA),
            Map.entry("return_sorting", DA_HOAN_TRA),
            Map.entry("returned", DA_HOAN_TRA)
    );

    private final ShippingProperties properties;

    public Optional<ShipmentStatus> map(String maCode, String rawStatus) {
        String key = normalize(rawStatus);
        if (key.isEmpty()) {
            return Optional.empty();
        }

        ShippingProperties.Carrier carrier = properties.carrier(maCode);
        if (carrier != null) {
            for (Map.Entry<String, String> e : carrier.getStatusMap().entrySet()) {
                if (normalize(e.getKey()).equals(key)) {
                    return Optional.ofNullable(ShipmentStatus.fromName(e.getValue()));
                }
            }
        }

        return Optional.ofNullable(DEFAULTS.get(key));
    }

    static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT).replaceAll("[\\s\\-]+", "_");
    }
}
