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
import com.oldbook.repository.store.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PromotionManagementService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final String DUPLICATE_CODE_PLATFORM = "Mã Voucher đã tồn tại trên hệ thống. Vui lòng chọn mã khác";
    private static final String DUPLICATE_CODE_SHOP = "Mã Voucher đã tồn tại, vui lòng nhập mã code khác";

    private final PromotionRepository promotionRepository;
    private final VoucherUsageRepository voucherUsageRepository;
    private final StoreRepository storeRepository;

    @Transactional(readOnly = true)
    public PromotionListResponse getAll(String tuKhoa, String trangThai) {
        return list(null, tuKhoa, trangThai);
    }

    @Transactional(readOnly = true)
    public PromotionListResponse getAllOfStore(Integer maCH, String tuKhoa, String trangThai) {
        return list(maCH, tuKhoa, trangThai);
    }

    @Transactional(readOnly = true)
    public PromotionResponse getDetail(Integer maKM) {
        return toResponse(require(maKM, null));
    }

    @Transactional(readOnly = true)
    public PromotionResponse getDetailOfStore(Integer maCH, Integer maKM) {
        return toResponse(require(maKM, maCH));
    }

    @Transactional
    public PromotionResponse create(CreatePromotionRequest req) {
        return createIn(null, req);
    }

    @Transactional
    public PromotionResponse createInStore(Integer maCH, CreatePromotionRequest req) {
        return createIn(maCH, req);
    }

    @Transactional
    public PromotionResponse update(Integer maKM, UpdatePromotionRequest req) {
        return updateIn(null, maKM, req);
    }

    @Transactional
    public PromotionResponse updateInStore(Integer maCH, Integer maKM, UpdatePromotionRequest req) {
        return updateIn(maCH, maKM, req);
    }

    @Transactional
    public PromotionResponse changeStatus(Integer maKM, String trangThai) {
        return changeStatusIn(null, maKM, trangThai);
    }

    @Transactional
    public PromotionResponse changeStatusInStore(Integer maCH, Integer maKM, String trangThai) {
        return changeStatusIn(maCH, maKM, trangThai);
    }

    @Transactional
    public void delete(Integer maKM) {
        deleteIn(null, maKM);
    }

    @Transactional
    public void deleteInStore(Integer maCH, Integer maKM) {
        deleteIn(maCH, maKM);
    }

    private PromotionListResponse list(Integer maCH, String tuKhoa, String trangThai) {
        LocalDateTime now = LocalDateTime.now();
        Set<Integer> used = voucherUsageRepository.findUsedPromotionIds();
        String keyword = normalizeKeyword(tuKhoa);
        String statusFilter = trangThai == null || trangThai.isBlank() ? null : trangThai.trim();

        List<Promotion> promotions = maCH == null
                ? promotionRepository.findAllByLoaiVoucherOrderByMaKMDesc(VoucherType.VOUCHER_SAN.name())
                : promotionRepository.findAllByCuaHang_MaCHAndLoaiVoucherOrderByMaKMDesc(
                        maCH, VoucherType.VOUCHER_SHOP.name());

        List<PromotionResponse> all = promotions.stream()
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

    private PromotionResponse createIn(Integer maCH, CreatePromotionRequest req) {
        String duplicateMessage = maCH == null ? DUPLICATE_CODE_PLATFORM : DUPLICATE_CODE_SHOP;
        String code = req.getCode().trim().toUpperCase(Locale.ROOT);
        if (promotionRepository.existsByCodeIgnoreCase(code)) {
            throw new BusinessException(duplicateMessage);
        }
        validate(req);

        Promotion promotion = Promotion.builder()
                .cuaHang(maCH == null ? null : storeRepository.getReferenceById(maCH))
                .code(code)
                .loaiVoucher((maCH == null ? VoucherType.VOUCHER_SAN : VoucherType.VOUCHER_SHOP).name())
                .daSuDung(0)
                .trangThai(PromotionStatus.HOAT_DONG.name())
                .build();
        apply(promotion, req);

        try {
            promotion = promotionRepository.saveAndFlush(promotion);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(duplicateMessage);
        }
        return toResponse(promotion);
    }

    private PromotionResponse updateIn(Integer maCH, Integer maKM, UpdatePromotionRequest req) {
        Promotion promotion = requireForUpdate(maKM, maCH);
        validate(req);

        int used = promotion.getDaSuDung() == null ? 0 : promotion.getDaSuDung();
        if (req.getSoLuong() < used) {
            throw new BusinessException(
                    "Số lượng phát hành không được nhỏ hơn số lượt đã sử dụng (" + used + ")");
        }

        apply(promotion, req);
        return toResponse(promotionRepository.save(promotion));
    }

    private PromotionResponse changeStatusIn(Integer maCH, Integer maKM, String trangThai) {
        Promotion promotion = requireForUpdate(maKM, maCH);
        promotion.setTrangThai(PromotionStatus.valueOf(trangThai.trim()).name());
        return toResponse(promotionRepository.save(promotion));
    }

    private void deleteIn(Integer maCH, Integer maKM) {
        Promotion promotion = requireForUpdate(maKM, maCH);
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

    private Promotion require(Integer maKM, Integer maCH) {
        return promotionRepository.findById(maKM)
                .filter(p -> inScope(p, maCH))
                .orElseThrow(() -> notFound(maCH));
    }

    private Promotion requireForUpdate(Integer maKM, Integer maCH) {
        return promotionRepository.findByIdForUpdate(maKM)
                .filter(p -> inScope(p, maCH))
                .orElseThrow(() -> notFound(maCH));
    }

    private static boolean inScope(Promotion promotion, Integer maCH) {
        if (maCH == null) {
            return VoucherType.VOUCHER_SAN.name().equals(promotion.getLoaiVoucher());
        }
        return VoucherType.VOUCHER_SHOP.name().equals(promotion.getLoaiVoucher())
                && promotion.getCuaHang() != null
                && Objects.equals(promotion.getCuaHang().getMaCH(), maCH);
    }

    private static BusinessException notFound(Integer maCH) {
        return new BusinessException(maCH == null ? "Voucher toàn chuỗi không tồn tại" : "Voucher chi nhánh không tồn tại");
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
