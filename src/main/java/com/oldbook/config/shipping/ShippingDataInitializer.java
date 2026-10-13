package com.oldbook.config.shipping;

import com.oldbook.constant.shipping.CarrierStatus;
import com.oldbook.entity.shipping.ShippingCarrier;
import com.oldbook.repository.shipping.ShippingCarrierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

// Tạo sẵn danh sách đơn vị vận chuyển dùng chung toàn sàn nếu chưa có (theo ma_code)
@Component
@RequiredArgsConstructor
public class ShippingDataInitializer implements ApplicationRunner {

    private final ShippingCarrierRepository shippingCarrierRepository;

    @Override
    public void run(ApplicationArguments args) {
        List<ShippingCarrier> defaults = List.of(
                carrier("GHN", "Giao Hàng Nhanh", "25000", 2),
                carrier("GHTK", "Giao Hàng Tiết Kiệm", "20000", 3),
                carrier("VTP", "Viettel Post", "22000", 3),
                carrier("JT", "J&T Express", "18000", 4)
        );

        for (ShippingCarrier carrier : defaults) {
            if (!shippingCarrierRepository.existsByMaCode(carrier.getMaCode())) {
                shippingCarrierRepository.save(carrier);
            }
        }
    }

    private ShippingCarrier carrier(String maCode, String ten, String phi, int soNgay) {
        return ShippingCarrier.builder()
                .maCode(maCode)
                .tenDonVi(ten)
                .phiVanChuyen(new BigDecimal(phi))
                .thoiGianGiaoDuKien(soNgay)
                .trangThai(CarrierStatus.HOAT_DONG.name())
                .build();
    }
}
