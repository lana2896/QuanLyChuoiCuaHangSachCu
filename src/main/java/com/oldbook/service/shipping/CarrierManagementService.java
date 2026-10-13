package com.oldbook.service.shipping;

import com.oldbook.constant.shipping.CarrierStatus;
import com.oldbook.dto.shipping.UpdateCarrierRequest;
import com.oldbook.dto.shipping.CarrierResponse;
import com.oldbook.dto.shipping.CreateCarrierRequest;
import com.oldbook.entity.shipping.ShippingCarrier;
import com.oldbook.exception.common.BusinessException;
import com.oldbook.repository.shipping.ShippingCarrierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Quản lý / quản trị viên thêm và sửa đối tác vận chuyển.
 * Tạo hãng với mã X là đủ: trang /shipping/X và API /api/shipping/X/... dùng được ngay (định tuyến theo {maCode}).
 */
@Service
@RequiredArgsConstructor
public class CarrierManagementService {

    private final ShippingCarrierRepository donViRepository;
    private final ShippingCarrierResolver donViResolver;

    @Transactional(readOnly = true)
    public List<CarrierResponse> getAll() {
        return donViRepository.findAllByOrderByMaDVVCAsc().stream()
                .map(CarrierResponse::from)
                .toList();
    }

    @Transactional
    public CarrierResponse create(CreateCarrierRequest req) {
        String maCode = ShippingCarrierResolver.normalize(req.getMaCode());
        if (maCode == null) {
            throw new BusinessException("Mã đơn vị vận chuyển chỉ gồm chữ, số, '-' hoặc '_' (2-30 ký tự)");
        }
        if (donViRepository.existsByMaCode(maCode)) {
            throw new BusinessException("Mã đơn vị vận chuyển đã tồn tại: " + maCode);
        }

        ShippingCarrier dvvc = ShippingCarrier.builder()
                .maCode(maCode)
                .trangThai(CarrierStatus.HOAT_DONG.name())
                .build();
        apply(dvvc, req);

        return CarrierResponse.from(donViRepository.save(dvvc));
    }

    @Transactional
    public CarrierResponse update(String maCode, UpdateCarrierRequest req) {
        ShippingCarrier dvvc = donViResolver.require(maCode);
        apply(dvvc, req);
        return CarrierResponse.from(donViRepository.save(dvvc));
    }

    /** Ngừng hoạt động: khách không chọn được ở checkout, trang/API của hãng không tiếp nhận/cập nhật đơn được nữa. */
    @Transactional
    public CarrierResponse changeStatus(String maCode, String trangThai) {
        ShippingCarrier dvvc = donViResolver.require(maCode);
        CarrierStatus status;
        try {
            status = CarrierStatus.valueOf(trangThai.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new BusinessException("Trạng thái chỉ gồm: HOAT_DONG hoặc NGUNG_HOAT_DONG");
        }
        dvvc.setTrangThai(status.name());
        return CarrierResponse.from(donViRepository.save(dvvc));
    }

    private void apply(ShippingCarrier dvvc, UpdateCarrierRequest req) {
        dvvc.setTenDonVi(req.getTenDonVi().trim());
        dvvc.setPhiVanChuyen(req.getPhiVanChuyen());
        dvvc.setThoiGianGiaoDuKien(req.getThoiGianGiaoDuKien());
        dvvc.setSoDienThoai(blankToNull(req.getSoDienThoai()));
        dvvc.setEmail(blankToNull(req.getEmail()));
        dvvc.setThongTinTichHop(blankToNull(req.getThongTinTichHop()));
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
