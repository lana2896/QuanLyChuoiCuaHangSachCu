package com.oldbook.service.shipping;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Đồng bộ định kỳ trạng thái vận đơn. Mặc định bật; tắt bằng {@code shipping.sync.polling-enabled=false}. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "shipping.sync.polling-enabled", havingValue = "true", matchIfMissing = true)
public class ShipmentPollingScheduler {

    private final ShipmentTrackingService trackingService;

    @Scheduled(
            fixedDelayString = "${shipping.sync.polling-interval-ms:60000}",
            initialDelayString = "${shipping.sync.polling-interval-ms:60000}"
    )
    public void sync() {
        int changed = trackingService.pollActiveShipments();
        if (changed > 0) {
            log.info("Đồng bộ vận chuyển: {} vận đơn có thay đổi", changed);
        }
    }
}
