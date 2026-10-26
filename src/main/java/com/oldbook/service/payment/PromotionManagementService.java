package com.oldbook.service.payment;

import com.oldbook.constant.payment.DiscountType;
import com.oldbook.constant.payment.PromotionStatus;
import com.oldbook.constant.payment.VoucherType;
import com.oldbook.dto.payment.CreatePromotionRequest;
import com.oldbook.dto.payment.PromotionListResponse;
import com.oldbook.dto.payment.PromotionResponse;
import com.oldbook.dto.payment.PromotionStatsResponse;
import com.oldbook.dto.payment.UpdatePromotionRequest;
import com.oldbook.entity.payment.Promotion;
import com.oldbook.exception.common.BusinessException;
import com.oldbook.repository.payment.PromotionRepository;
import com.oldbook.repository.payment.VoucherUsageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PromotionManagementService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final String DUPLICATE_CODE = "Mã Voucher đã tồn tại trên hệ thống. Vui lòng chọn mã khác";

    private final PromotionRepository promotionRepository;
    private final VoucherUsageRepository voucherUsageRepository;

    @Transactional(readOnly = true)
    public PromotionListResponse getAll(String tuKhoa, String trangThai) {
        LocalDateTime now = LocalDateTime.now();
        Set<Integer> used = voucherUsageRepository.findUsedPromotionIds();
        String keyword = normalizeKeyword(tuKhoa);
        String statusFilter = trangThai == null || trangThai.isBlank() ? null : trangThai.trim();

        List<PromotionResponse> all = promotionRepository
                .findAllByLoaiVoucherOrderByMaKMDesc(VoucherType.VOUCHER_SAN.name()).stream()
                .map(p -> PromotionResponse.from(p, now, used.contains(p.getMaKM())))
                .toList();

        PromotionStatsResponse stats = PromotionStatsResponse.builder()
                .tong(all.size())
                .dangApDung(count(all, PromotionResponse.DISPLAY_ACTIVE))
                .tamDung(count(all, PromotionResponse.DISPLAY_PAUSED))
                .hetHieuLuc(count(all, PromotionResponse.DISPLAY_EXPIRED)
                        + count(all, PromotionResponse.DISPLAY_EXHAUSTED))
                .build();

        List<PromotionResponse> filtered = all.stream()
                .filter(v -> statusFilter == null || statusFilter.equals(v.getTrangThaiHienThi()))
                .filter(v -> keyword == null
                        || lower(v.getCode()).contains(keyword)
                        || lower(v.getTenKhuyenMai()).contains(keyword))
                .toList();

        return PromotionListResponse.builder().thongKe(stats).danhSach(filtered).build();
    }

    @Transactional(readOnly = true)
    public PromotionResponse getDetail(Integer maKM) {
        return toResponse(require(maKM));
    }

    @Transactional
    public PromotionResponse create(CreatePromotionRequest req) {
        String code = req.getCode().trim().toUpperCase(Locale.ROOT);
        if (promotionRepository.existsByCodeIgnoreCase(code)) {
            throw new BusinessException(DUPLICATE_CODE);
        }
        validate(req);

        Promotion promotion = Promotion.builder()
                .cuaHang(null)
                .code(code)
                .loaiVoucher(VoucherType.VOUCHER_SAN.name())
                .daSuDung(0)
                .trangThai(PromotionStatus.HOAT_DONG.name())
                .build();
        apply(promotion, req);

        try {
            promotion = promotionRepository.saveAndFlush(promotion);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(DUPLICATE_CODE);
        }
        return toResponse(promotion);
    }

    @Transactional
    public PromotionResponse update(Integer maKM, UpdatePromotionRequest req) {
        Promotion promotion = requireForUpdate(maKM);
        validate(req);

        int used = promotion.getDaSuDung() == null ? 0 : promotion.getDaSuDung();
        if (req.getSoLuong() < used) {
            throw new BusinessException(
                    "Số lượng phát hành không được nhỏ hơn số lượt đã sử dụng (" + used + ")");
        }

        apply(promotion, req);
        return toResponse(promotionRepository.save(promotion));
    }

    @Transactional
    public PromotionResponse changeStatus(Integer maKM, String trangThai) {
        Promotion promotion = requireForUpdate(maKM);
        promotion.setTrangThai(PromotionStatus.valueOf(trangThai.trim()).name());
        return toResponse(promotionRepository.save(promotion));
    }

    @Transactional
    public void delete(Integer maKM) {
        Promotion promotion = requireForUpdate(maKM);
        if (voucherUsageRepository.existsByKhuyenMai_MaKM(maKM)) {
            throw new BusinessException(
                    "Voucher đã được sử dụng trong đơn hàng nên không thể xóa. Hãy tạm dừng voucher này");
        }
        promotionRepository.delete(promotion);
    }

    private void validate(UpdatePromotionRequest req) {
        if (!req.getNgayKetThuc().isAfter(req.getNgayBatDau())) {
            throw new BusinessException("Thời gian kết thúc phải sau thời gian bắt đầu");
        }
        if (DiscountType.THEO_PHAN_TRAM.name().equals(req.getLoaiGiamGia())
                && req.getGiaTriGiam().compareTo(HUNDRED) > 0) {
            throw new BusinessException("Giá trị giảm theo phần trăm không được vượt quá 100");
        }
    }

    private void apply(Promotion promotion, UpdatePromotionRequest req) {
        boolean percent = DiscountType.THEO_PHAN_TRAM.name().equals(req.getLoaiGiamGia());
        BigDecimal maxDiscount = req.getGiaTriGiamToiDa();

        promotion.setTenKhuyenMai(req.getTenKhuyenMai().trim());
        promotion.setLoaiGiamGia(req.getLoaiGiamGia());
        promotion.setGiaTriGiam(req.getGiaTriGiam());
        promotion.setGiaTriGiamToiDa(percent && maxDiscount != null && maxDiscount.signum() > 0 ? maxDiscount : null);
        promotion.setGiaTriDonToiThieu(req.getGiaTriDonToiThieu() == null ? BigDecimal.ZERO : req.getGiaTriDonToiThieu());
        promotion.setSoLuong(req.getSoLuong());
        promotion.setNgayBatDau(req.getNgayBatDau());
        promotion.setNgayKetThuc(req.getNgayKetThuc());
    }

    private Promotion require(Integer maKM) {
        return promotionRepository.findById(maKM)
                .filter(p -> VoucherType.VOUCHER_SAN.name().equals(p.getLoaiVoucher()))
                .orElseThrow(() -> new BusinessException("Voucher Sàn không tồn tại"));
    }

    private Promotion requireForUpdate(Integer maKM) {
        return promotionRepository.findByIdForUpdate(maKM)
                .filter(p -> VoucherType.VOUCHER_SAN.name().equals(p.getLoaiVoucher()))
                .orElseThrow(() -> new BusinessException("Voucher Sàn không tồn tại"));
    }

    private PromotionResponse toResponse(Promotion promotion) {
        return PromotionResponse.from(promotion, LocalDateTime.now(),
                voucherUsageRepository.existsByKhuyenMai_MaKM(promotion.getMaKM()));
    }

    private static long count(List<PromotionResponse> list, String status) {
        return list.stream().filter(v -> status.equals(v.getTrangThaiHienThi())).count();
    }

    private static String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim().toLowerCase(Locale.ROOT);
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
