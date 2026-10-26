package com.oldbook.service.payment;

import com.oldbook.constant.order.OrderStatus;
import com.oldbook.constant.payment.DiscountType;
import com.oldbook.constant.payment.PromotionStatus;
import com.oldbook.constant.payment.VoucherType;
import com.oldbook.dto.payment.VoucherOptionResponse;
import com.oldbook.entity.cart.Cart;
import com.oldbook.entity.cart.CartItem;
import com.oldbook.entity.identity.User;
import com.oldbook.entity.order.Order;
import com.oldbook.entity.order.StoreOrder;
import com.oldbook.entity.payment.Promotion;
import com.oldbook.entity.payment.VoucherUsage;
import com.oldbook.exception.common.BusinessException;
import com.oldbook.repository.cart.CartItemRepository;
import com.oldbook.repository.cart.CartRepository;
import com.oldbook.repository.order.OrderRepository;
import com.oldbook.repository.order.StoreOrderRepository;
import com.oldbook.repository.payment.PromotionRepository;
import com.oldbook.repository.payment.VoucherUsageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class VoucherService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final String INVALID_VOUCHER = "Voucher không còn hiệu lực hoặc không hợp lệ";

    private final PromotionRepository promotionRepository;
    private final VoucherUsageRepository voucherUsageRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;
    private final StoreOrderRepository storeOrderRepository;

    public record AppliedVoucher(Promotion promotion, BigDecimal giamGia) {}

    @Transactional(readOnly = true)
    public List<VoucherOptionResponse> listAvailable(Integer maND, List<Integer> maCTGioHangs) {
        BigDecimal tongTienHang = selectedSubtotal(maND, maCTGioHangs);
        return promotionRepository
                .findAvailable(VoucherType.VOUCHER_SAN.name(), PromotionStatus.HOAT_DONG.name(), LocalDateTime.now())
                .stream()
                .map(p -> toOption(p, tongTienHang))
                .toList();
    }

    @Transactional(readOnly = true)
    public VoucherOptionResponse preview(Integer maND, String code, List<Integer> maCTGioHangs) {
        BigDecimal tongTienHang = selectedSubtotal(maND, maCTGioHangs);
        Promotion promotion = findByCode(code);
        validateUsable(promotion);
        validateMinimum(promotion, tongTienHang);
        return toOption(promotion, tongTienHang);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public AppliedVoucher lockAndApply(String code, BigDecimal tongTienHang) {
        if (code == null || code.isBlank()) {
            throw new BusinessException("Vui lòng nhập mã voucher");
        }
        Promotion promotion = promotionRepository.findByCodeForUpdate(code.trim())
                .filter(p -> VoucherType.VOUCHER_SAN.name().equals(p.getLoaiVoucher()))
                .orElseThrow(() -> new BusinessException("Mã voucher không tồn tại hoặc không hợp lệ"));
        validateUsable(promotion);
        validateMinimum(promotion, tongTienHang);
        return new AppliedVoucher(promotion, computeDiscount(promotion, tongTienHang));
    }

    public List<BigDecimal> allocate(BigDecimal giamGia, List<BigDecimal> tienHangTheoCuaHang) {
        int size = tienHangTheoCuaHang.size();
        List<BigDecimal> shares = new ArrayList<>(size);
        BigDecimal total = tienHangTheoCuaHang.stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        if (giamGia.signum() <= 0 || total.signum() <= 0) {
            for (int i = 0; i < size; i++) {
                shares.add(BigDecimal.ZERO);
            }
            return shares;
        }

        BigDecimal assigned = BigDecimal.ZERO;
        for (BigDecimal tienHang : tienHangTheoCuaHang) {
            BigDecimal share = giamGia.multiply(tienHang).divide(total, 0, RoundingMode.DOWN);
            shares.add(share);
            assigned = assigned.add(share);
        }

        BigDecimal remainder = giamGia.subtract(assigned);
        for (int i = 0; remainder.signum() > 0 && i < size; i++) {
            if (shares.get(i).compareTo(tienHangTheoCuaHang.get(i)) < 0) {
                shares.set(i, shares.get(i).add(BigDecimal.ONE));
                remainder = remainder.subtract(BigDecimal.ONE);
            }
        }
        return shares;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordUsage(AppliedVoucher applied, Order order, User nguoiDung) {
        Promotion promotion = applied.promotion();
        promotion.setDaSuDung(used(promotion) + 1);
        promotionRepository.save(promotion);

        voucherUsageRepository.save(VoucherUsage.builder()
                .khuyenMai(promotion)
                .donHang(order)
                .nguoiDung(nguoiDung)
                .giaTriGiamThucTe(applied.giamGia())
                .build());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void restoreIfFullyCancelled(Integer maDH) {
        Order order = orderRepository.findById(maDH).orElse(null);
        if (order == null || order.getKhuyenMaiSan() == null) {
            return;
        }

        List<StoreOrder> children = storeOrderRepository.findAllByDonHang_MaDHOrderByMaDHCHAsc(maDH);
        boolean allCancelled = !children.isEmpty() && children.stream()
                .allMatch(c -> OrderStatus.DA_HUY.name().equals(c.getTrangThaiDonHang()));
        if (!allCancelled) {
            return;
        }

        promotionRepository.findByIdForUpdate(order.getKhuyenMaiSan().getMaKM()).ifPresent(promotion -> {
            promotion.setDaSuDung(Math.max(0, used(promotion) - 1));
            promotionRepository.save(promotion);
        });
    }

    private Promotion findByCode(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException("Vui lòng nhập mã voucher");
        }
        return promotionRepository.findByCodeIgnoreCase(code.trim())
                .filter(p -> VoucherType.VOUCHER_SAN.name().equals(p.getLoaiVoucher()))
                .orElseThrow(() -> new BusinessException("Mã voucher không tồn tại hoặc không hợp lệ"));
    }

    private void validateUsable(Promotion promotion) {
        LocalDateTime now = LocalDateTime.now();
        boolean inTime = promotion.getNgayBatDau() != null && promotion.getNgayKetThuc() != null
                && !now.isBefore(promotion.getNgayBatDau())
                && !now.isAfter(promotion.getNgayKetThuc());

        if (!PromotionStatus.HOAT_DONG.name().equals(promotion.getTrangThai()) || !inTime) {
            throw new BusinessException(INVALID_VOUCHER);
        }
        if (used(promotion) >= (promotion.getSoLuong() == null ? 0 : promotion.getSoLuong())) {
            throw new BusinessException("Voucher đã hết lượt sử dụng");
        }
    }

    private void validateMinimum(Promotion promotion, BigDecimal tongTienHang) {
        BigDecimal minimum = nz(promotion.getGiaTriDonToiThieu());
        if (tongTienHang.compareTo(minimum) < 0) {
            throw new BusinessException(
                    "Đơn hàng chưa đạt giá trị tối thiểu " + money(minimum) + " để áp dụng voucher");
        }
    }

    private BigDecimal computeDiscount(Promotion promotion, BigDecimal base) {
        BigDecimal value = nz(promotion.getGiaTriGiam());
        BigDecimal discount = DiscountType.THEO_PHAN_TRAM.name().equals(promotion.getLoaiGiamGia())
                ? base.multiply(value).divide(HUNDRED, 0, RoundingMode.DOWN)
                : value.setScale(0, RoundingMode.DOWN);

        BigDecimal cap = promotion.getGiaTriGiamToiDa();
        if (cap != null && cap.signum() > 0 && discount.compareTo(cap) > 0) {
            discount = cap.setScale(0, RoundingMode.DOWN);
        }
        return discount.min(base).max(BigDecimal.ZERO);
    }

    private VoucherOptionResponse toOption(Promotion promotion, BigDecimal tongTienHang) {
        BigDecimal minimum = nz(promotion.getGiaTriDonToiThieu());
        boolean eligible = tongTienHang.compareTo(minimum) >= 0;

        return VoucherOptionResponse.builder()
                .code(promotion.getCode())
                .tenKhuyenMai(promotion.getTenKhuyenMai())
                .loaiGiamGia(promotion.getLoaiGiamGia())
                .giaTriGiam(promotion.getGiaTriGiam())
                .giaTriGiamToiDa(promotion.getGiaTriGiamToiDa())
                .giaTriDonToiThieu(minimum)
                .ngayKetThuc(promotion.getNgayKetThuc())
                .conLai(Math.max(0, (promotion.getSoLuong() == null ? 0 : promotion.getSoLuong()) - used(promotion)))
                .daDuDieuKien(eligible)
                .giamDuKien(eligible ? computeDiscount(promotion, tongTienHang) : BigDecimal.ZERO)
                .lyDoKhongApDung(eligible ? null
                        : "Cần mua thêm " + money(minimum.subtract(tongTienHang)) + " để áp dụng")
                .build();
    }

    private BigDecimal selectedSubtotal(Integer maND, List<Integer> maCTGioHangs) {
        Cart cart = cartRepository.findByMaND(maND)
                .orElseThrow(() -> new BusinessException("Giỏ hàng đang trống"));

        Set<Integer> selected = new HashSet<>(maCTGioHangs);
        List<CartItem> items = cartItemRepository.findAllByGioHang(cart.getMaGioHang()).stream()
                .filter(ct -> selected.contains(ct.getMaCTGioHang()))
                .toList();

        if (items.isEmpty()) {
            throw new BusinessException("Sản phẩm được chọn không còn trong giỏ hàng");
        }

        return items.stream()
                .map(ct -> nz(ct.getSach().getGiaBanCu()).multiply(BigDecimal.valueOf(ct.getSoLuong())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static int used(Promotion promotion) {
        return promotion.getDaSuDung() == null ? 0 : promotion.getDaSuDung();
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String money(BigDecimal value) {
        return String.format(Locale.US, "%,d đ", value.setScale(0, RoundingMode.CEILING).longValue());
    }
}
