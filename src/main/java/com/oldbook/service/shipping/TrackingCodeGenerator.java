package com.oldbook.service.shipping;

import com.oldbook.repository.shipping.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Sinh mã vận đơn nội bộ cho các đơn vị vận chuyển không tự cấp mã (giả lập, cổng đối tác). */
@Component
@RequiredArgsConstructor
public class TrackingCodeGenerator {

    private final ShipmentRepository shipmentRepository;

    public String generate(String maCode, Integer maDHCH) {
        String prefix = maCode == null || maCode.isBlank() ? "VC" : maCode.trim().toUpperCase();

        String code;
        do {
            String random = UUID.randomUUID().toString()
                    .replace("-", "").substring(0, 6).toUpperCase();
            code = prefix + String.format("%08d", maDHCH) + random;
        } while (shipmentRepository.existsByMaVanDon(code));

        return code;
    }
}
