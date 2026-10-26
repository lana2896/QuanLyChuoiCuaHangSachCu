package com.oldbook.config.shipping;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@ConfigurationProperties(prefix = "shipping")
public class ShippingProperties {

    /** Số lần giao thất bại tối đa; lần thất bại thứ N này thì hệ thống tự chuyển hoàn hàng. */
    private int maxDeliveryAttempts = 3;

    private Sync sync = new Sync();

    /** Khóa là mã đơn vị vận chuyển (ShippingCarrier.maCode), không phân biệt hoa thường. */
    private Map<String, Carrier> carriers = new HashMap<>();

    public Carrier carrier(String maCode) {
        if (maCode == null) {
            return null;
        }
        for (Map.Entry<String, Carrier> e : carriers.entrySet()) {
            if (e.getKey().equalsIgnoreCase(maCode.trim())) {
                return e.getValue();
            }
        }
        return null;
    }

    @Getter
    @Setter
    public static class Sync {
        /**
         * portal: hãng tự cập nhật trên cổng đối tác /shipping/{maCode}.
         * simulator: giả lập tự đẩy trạng thái (dev/demo, không dùng cùng cổng đối tác).
         * rest: gọi API thật theo cấu hình từng đơn vị.
         * Giá trị thực tế đặt trong application.properties; mặc định ở đây giữ simulator cho test/cấu hình cũ.
         */
        private String provider = "simulator";
        private boolean pollingEnabled = true;
        private long pollingIntervalMs = 60_000;
        private int batchSize = 100;
    }

    @Getter
    @Setter
    public static class Carrier {
        private String baseUrl;
        private String apiToken;
        /** Tên header chứa token khi gọi API của đơn vị vận chuyển. */
        private String tokenHeader = "Token";

        private String createPath = "/orders";
        /** Có thể dùng {maVanDon}. */
        private String trackPath = "/orders/{maVanDon}";
        /** JSON pointer tới mã vận đơn trong phản hồi tạo đơn. */
        private String trackingCodePointer = "/data/order_code";
        /** JSON pointer tới trạng thái / ghi chú trong phản hồi tra cứu. */
        private String statusPointer = "/data/status";
        private String notePointer = "/data/note";

        /** Ánh xạ riêng trạng thái gốc -> ShipmentStatus (ghi đè bảng mặc định). */
        private Map<String, String> statusMap = new HashMap<>();
    }
}
