package com.oldbook.service.order;

import com.oldbook.constant.shipping.CarrierStatus;
import com.oldbook.dto.shipping.ShippingCarrierResponse;
import com.oldbook.repository.shipping.ShippingCarrierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShippingCarrierService {

    private final ShippingCarrierRepository shippingCarrierRepository;

    /** Danh sách đơn vị vận chuyển dùng chung toàn sàn, áp dụng cho mọi cửa hàng. */
    @Transactional(readOnly = true)
    public List<ShippingCarrierResponse> getActiveCarriers() {
        return shippingCarrierRepository
                .findAllByTrangThaiOrderByTenDonViAsc(CarrierStatus.HOAT_DONG.name())
                .stream()
                .map(c -> ShippingCarrierResponse.builder()
                        .maDVVC(c.getMaDVVC())
                        .maCode(c.getMaCode())
                        .tenDonVi(c.getTenDonVi())
                        .phiVanChuyen(c.getPhiVanChuyen())
                        .thoiGianGiaoDuKien(c.getThoiGianGiaoDuKien())
                        .build())
                .toList();
    }
}
