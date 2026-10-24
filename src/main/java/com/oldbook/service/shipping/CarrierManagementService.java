package com.oldbook.service.shipping;

import com.oldbook.constant.shipping.CarrierStatus;
import com.oldbook.constant.shipping.ShipmentStatus;
import com.oldbook.dto.shipping.UpdateCarrierRequest;
import com.oldbook.dto.shipping.CarrierListResponse;
import com.oldbook.dto.shipping.CarrierDetailResponse;
import com.oldbook.dto.shipping.CreateCarrierRequest;
import com.oldbook.dto.shipping.CarrierStatsResponse;
import com.oldbook.entity.shipping.ShippingCarrier;
import com.oldbook.exception.common.BusinessException;
import com.oldbook.repository.shipping.ShippingCarrierRepository;
import com.oldbook.repository.shipping.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class CarrierManagementService {

    private static final List<String> TRANG_THAI_KET_THUC = List.of(
            ShipmentStatus.DA_GIAO.name(),
            ShipmentStatus.DA_HOAN_TRA.name()
    );

    private final ShippingCarrierRepository donViRepository;
    private final ShippingCarrierResolver donViResolver;
    private final ShipmentRepository shipmentRepository;

    @Transactional(readOnly = true)
    public CarrierListResponse getAll(String tuKhoa, String trangThai, String baseUrl) {
        CarrierStatus locTrangThai = parseStatus(trangThai, true);
        String tuKhoaChuan = normalizeSearch(tuKhoa);

        List<ShippingCarrier> tatCa = donViRepository.findAllByOrderByMaDVVCAsc();
        Map<Integer, Long> tongVanDon = toMap(shipmentRepository.demVanDonTheoDonVi());
        Map<Integer, Long> dangXuLy = toMap(shipmentRepository.demVanDonDangXuLyTheoDonVi(TRANG_THAI_KET_THUC));
        Map<Integer, Long> daGiao = toMap(shipmentRepository.demVanDonTheoDonViVaTrangThai(ShipmentStatus.DA_GIAO.name()));

        long soDangHopTac = tatCa.stream().filter(ShippingCarrierResolver::isActive).count();
        CarrierStatsResponse thongKe = CarrierStatsResponse.builder()
                .tongDoiTac(tatCa.size())
                .dangHopTac(soDangHopTac)
                .tamDung(tatCa.size() - soDangHopTac)
                .vanDonDangXuLy(dangXuLy.values().stream().mapToLong(Long::longValue).sum())
                .build();

        List<CarrierDetailResponse> danhSach = tatCa.stream()
                .filter(dvvc -> locTrangThai == null || locTrangThai.name().equalsIgnoreCase(dvvc.getTrangThai()))
                .filter(dvvc -> tuKhoaChuan == null || containsKeyword(dvvc, tuKhoaChuan))
                .map(dvvc -> CarrierDetailResponse.from(dvvc, baseUrl,
                        tongVanDon.getOrDefault(dvvc.getMaDVVC(), 0L),
                        dangXuLy.getOrDefault(dvvc.getMaDVVC(), 0L),
                        daGiao.getOrDefault(dvvc.getMaDVVC(), 0L)))
                .toList();

        return CarrierListResponse.builder()
                .thongKe(thongKe)
                .danhSach(danhSach)
                .build();
    }

    @Transactional(readOnly = true)
    public CarrierDetailResponse getDetail(String maCode, String baseUrl) {
        return toResponse(donViResolver.require(maCode), baseUrl);
    }

    @Transactional
    public CarrierDetailResponse create(CreateCarrierRequest req, String baseUrl) {
        String maCode = ShippingCarrierResolver.normalize(req.getMaCode());
        if (maCode == null) {
            throw new BusinessException("Mã đơn vị vận chuyển chỉ gồm chữ, số, '-' hoặc '_' (2-30 ký tự)");
        }
        if (donViRepository.existsByMaCode(maCode)) {
            throw new BusinessException("Mã đơn vị vận chuyển đã tồn tại: " + maCode);
        }
        String tenDonVi = req.getTenDonVi().trim();
        ensureUniqueName(tenDonVi, null);

        ShippingCarrier dvvc = ShippingCarrier.builder()
                .maCode(maCode)
                .trangThai(CarrierStatus.HOAT_DONG.name())
                .ngayCapNhatTrangThai(LocalDateTime.now())
                .build();
        apply(dvvc, req);

        return toResponse(donViRepository.save(dvvc), baseUrl);
    }

    @Transactional
    public CarrierDetailResponse update(String maCode, UpdateCarrierRequest req, String baseUrl) {
        ShippingCarrier dvvc = donViResolver.require(maCode);
        ensureUniqueName(req.getTenDonVi().trim(), dvvc.getMaDVVC());
        apply(dvvc, req);
        return toResponse(donViRepository.save(dvvc), baseUrl);
    }

    @Transactional
    public CarrierDetailResponse changeStatus(String maCode, String trangThai, String lyDo, String baseUrl) {
        ShippingCarrier dvvc = donViResolver.require(maCode);
        CarrierStatus moi = parseStatus(trangThai, false);

        if (moi.name().equalsIgnoreCase(dvvc.getTrangThai())) {
            throw new BusinessException(moi == CarrierStatus.HOAT_DONG
                    ? "Đơn vị vận chuyển đang hợp tác"
                    : "Đơn vị vận chuyển đã tạm dừng hợp tác");
        }

        if (moi == CarrierStatus.NGUNG_HOAT_DONG) {
            String lyDoChuan = blankToNull(lyDo);
            if (lyDoChuan == null) {
                throw new BusinessException("Vui lòng nhập lý do tạm dừng hợp tác");
            }
            dvvc.setLyDoTamDung(lyDoChuan);
        } else {
            dvvc.setLyDoTamDung(null);
        }

        dvvc.setTrangThai(moi.name());
        dvvc.setNgayCapNhatTrangThai(LocalDateTime.now());
        return toResponse(donViRepository.save(dvvc), baseUrl);
    }

    private CarrierDetailResponse toResponse(ShippingCarrier dvvc, String baseUrl) {
        Integer id = dvvc.getMaDVVC();
        if (id == null) {
            return CarrierDetailResponse.from(dvvc, baseUrl, 0, 0, 0);
        }
        return CarrierDetailResponse.from(dvvc, baseUrl,
                shipmentRepository.countByDonViVanChuyen_MaDVVC(id),
                shipmentRepository.countByDonViVanChuyen_MaDVVCAndTrangThaiVanChuyenNotIn(id, TRANG_THAI_KET_THUC),
                shipmentRepository.countByDonViVanChuyen_MaDVVCAndTrangThaiVanChuyen(id, ShipmentStatus.DA_GIAO.name()));
    }

    private void ensureUniqueName(String tenDonVi, Integer boQuaMaDVVC) {
        String tenChuan = normalizeSearch(tenDonVi);
        boolean trung = donViRepository.findAll().stream()
                .filter(dvvc -> !Objects.equals(dvvc.getMaDVVC(), boQuaMaDVVC))
                .anyMatch(dvvc -> Objects.equals(normalizeSearch(dvvc.getTenDonVi()), tenChuan));
        if (trung) {
            throw new BusinessException("Tên đơn vị vận chuyển đã tồn tại: " + tenDonVi);
        }
    }

    private void apply(ShippingCarrier dvvc, UpdateCarrierRequest req) {
        dvvc.setTenDonVi(req.getTenDonVi().trim());
        dvvc.setPhiVanChuyen(req.getPhiVanChuyen());
        dvvc.setThoiGianGiaoDuKien(req.getThoiGianGiaoDuKien());
        dvvc.setNguoiLienHe(blankToNull(req.getNguoiLienHe()));
        dvvc.setSoDienThoai(blankToNull(req.getSoDienThoai()));
        dvvc.setEmail(blankToNull(req.getEmail()));
        dvvc.setDiaChi(blankToNull(req.getDiaChi()));
        dvvc.setWebsite(blankToNull(req.getWebsite()));
        dvvc.setGhiChu(blankToNull(req.getGhiChu()));
        dvvc.setThongTinTichHop(blankToNull(req.getThongTinTichHop()));
    }

    private static CarrierStatus parseStatus(String trangThai, boolean choPhepRong) {
        if (trangThai == null || trangThai.isBlank()) {
            if (choPhepRong) {
                return null;
            }
            throw new BusinessException("Trạng thái không được để trống");
        }
        try {
            return CarrierStatus.valueOf(trangThai.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Trạng thái chỉ gồm: HOAT_DONG hoặc NGUNG_HOAT_DONG");
        }
    }

    private static boolean containsKeyword(ShippingCarrier dvvc, String tuKhoa) {
        return Stream.of(dvvc.getMaCode(), dvvc.getTenDonVi(), dvvc.getNguoiLienHe(),
                        dvvc.getSoDienThoai(), dvvc.getEmail())
                .map(CarrierManagementService::normalizeSearch)
                .filter(Objects::nonNull)
                .anyMatch(giaTri -> giaTri.contains(tuKhoa));
    }

    private static String normalizeSearch(String value) {
        String v = blankToNull(value);
        if (v == null) {
            return null;
        }
        String khongDau = Normalizer.normalize(v, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D');
        return khongDau.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private static Map<Integer, Long> toMap(List<Object[]> rows) {
        Map<Integer, Long> result = new HashMap<>();
        for (Object[] row : rows) {
            if (row[0] != null && row[1] != null) {
                result.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue());
            }
        }
        return result;
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
