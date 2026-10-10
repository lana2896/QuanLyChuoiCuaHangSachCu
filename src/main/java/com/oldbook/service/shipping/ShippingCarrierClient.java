package com.oldbook.service.shipping;

import com.oldbook.entity.shipping.Shipment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Cổng giao tiếp chủ động với hệ thống của đơn vị vận chuyển
 * (tạo vận đơn khi cửa hàng xác nhận, tra cứu trạng thái khi đồng bộ định kỳ).
 * Hệ thống luôn chủ động gọi API; không nhận webhook.
 */
public interface ShippingCarrierClient {

    /** Chuyển đơn cho đơn vị vận chuyển tiếp nhận; trả về mã vận đơn do họ cấp. */
    CarrierShipment createShipment(CarrierShipmentRequest request);

    /** Tra cứu trạng thái hiện tại của vận đơn. Rỗng nếu đơn vị vận chuyển chưa có thông tin mới. */
    Optional<CarrierTracking> track(Shipment shipment);

    record CarrierShipmentRequest(
            String maCode,
            Integer maDHCH,
            String tenNguoiNhan,
            String soDienThoaiNhan,
            String diaChiGiao,
            /** Số tiền đơn vị vận chuyển phải thu hộ (COD). */
            BigDecimal tienThuHo,
            String ghiChu
    ) {}

    record CarrierShipment(String maVanDon) {}

    record CarrierTracking(String trangThaiGoc, String ghiChu, LocalDateTime thoiGian) {}
}
