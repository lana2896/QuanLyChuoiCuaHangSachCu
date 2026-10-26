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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class VoucherService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final String INVALID_VOUCHER = "Voucher không còn hiệu lực hoặc không hợp lệ";
    private static final String UNKNOWN_VOUCHER = "Mã voucher không tồn tại hoặc không hợp lệ";
    private static final String WRONG_STORE = "Voucher không áp dụng cho cửa hàng này";

    private final PromotionRepository promotionRepository;
    private final VoucherUsageRepository voucherUsageRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;
    private final StoreOrderRepository storeOrderRepository;

    public record AppliedVoucher(Promotion promotion, BigDecimal giamGia) {}

    @Transactional(readOnly = true)
    public List<VoucherOptionResponse> listAvailable(Integer maND, List<Integer> maCTGioHangs) {
        BigDecimal tongTienHang = subtotal(selectedItems(maND, maCTGioHangs));
        return promotionRepository
                .findAvailable(VoucherType.VOUCHER_SAN.name(), PromotionStatus.HOAT_DONG.name(), LocalDateTime.now())
                .stream()
                .map(p -> toOption(p, tongTienHang))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VoucherOptionResponse> listAvailableShop(Integer maND, List<Integer> maCTGioHangs) {
        Map<Integer, BigDecimal> subtotals = subtotalsByStore(selectedItems(maND, maCTGioHangs));
        return promotionRepository
                .findAvailableByStores(VoucherType.VOUCHER_SHOP.name(), new ArrayList<>(subtotals.keySet()),
                        PromotionStatus.HOAT_DONG.name(), LocalDateTime.now())
                .stream()
                .map(p -> toOption(p, subtotals.get(p.getCuaHang().getMaCH())))
                .toList();
    }

    @Transactional(readOnly = true)
    public VoucherOptionResponse preview(Integer maND, String code, List<Integer> maCTGioHangs) {
        BigDecimal tongTienHang = subtotal(selectedItems(maND, maCTGioHangs));
        Promotion promotion = findByCode(code, VoucherType.VOUCHER_SAN);
        validateUsable(promotion);
        validateMinimum(promotion, tongTienHang);
        return toOption(promotion, tongTienHang);
    }

    @Transactional(readOnly = true)
    public VoucherOptionResponse previewShop(Integer maND, String code, Integer maCH, List<Integer> maCTGioHangs) {
        if (maCH == null) {
            throw new BusinessException("Vui lòng chọn cửa hàng áp dụng voucher");
        }
        BigDecimal tienHangCH = subtotalsByStore(selectedItems(maND, maCTGioHangs)).get(maCH);
        if (tienHangCH == null) {
            throw new BusinessException("Cửa hàng không có trong giỏ hàng của bạn");
        }
        Promotion promotion = findByCode(code, VoucherType.VOUCHER_SHOP);
        requireStore(promotion, maCH);
        validateUsable(promotion);
        validateMinimum(promotion, tienHangCH);
        return toOption(promotion, tienHangCH);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public AppliedVoucher lockAndApply(String code, BigDecimal tongTienHang) {
        Promotion promotion = lockByCode(code, VoucherType.VOUCHER_SAN);
        validateUsable(promotion);
        validateMinimum(promotion, tongTienHang);
        return new AppliedVoucher(promotion, computeDiscount(promotion, tongTienHang));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public AppliedVoucher lockAndApplyShop(String code, Integer maCH, BigDecimal tienHangCH) {
        Promotion promotion = lockByCode(code, VoucherType.VOUCHER_SHOP);
        requireStore(promotion, maCH);
        validateUsable(promotion);
        validateMinimum(promotion, tienHangCH);
        return new AppliedVoucher(promotion, computeDiscount(promotion, tienHangCH));
    }

    public AppliedVoucher capDiscount(AppliedVoucher applied, BigDecimal max) {
        BigDecimal capped = applied.giamGia().min(max).max(BigDecimal.ZERO);
        return new AppliedVoucher(applied.promotion(), capped);
    }

    public List<BigDecimal> allocate(BigDecimal giamGia, List<BigDecimal> tienHangTheoCuaHang, List<BigDecimal> toiDaTheoCuaHang) {
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
        for (int i = 0; i < size; i++) {
            BigDecimal share = giamGia.multiply(tienHangTheoCuaHang.get(i))
                    .divide(total, 0, RoundingMode.DOWN)
                    .min(toiDaTheoCuaHang.get(i).max(BigDecimal.ZERO));
            shares.add(share);
            assigned = assigned.add(share);
        }

        BigDecimal remainder = giamGia.subtract(assigned);
        for (int i = 0; remainder.signum() > 0 && i < size; i++) {
            BigDecimal room = toiDaTheoCuaHang.get(i).subtract(shares.get(i));
            if (room.signum() > 0) {
                BigDecimal extra = room.min(remainder);
                shares.set(i, shares.get(i).add(extra));
                remainder = remainder.subtract(extra);
            }
        }
        return shares;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordUsage(AppliedVoucher applied, Order order, StoreOrder storeOrder, User nguoiDung) {
        Promotion promotion = applied.promotion();
        promotion.setDaSuDung(used(promotion) + 1);
        promotionRepository.save(promotion);

        voucherUsageRepository.save(VoucherUsage.builder()
                .khuyenMai(promotion)
                .donHang(order)
                .donHangCuaHang(storeOrder)
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

        releaseUsage(order.getKhuyenMaiSan());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void restoreShopVoucher(StoreOrder storeOrder) {
        if (storeOrder.getKhuyenMaiShop() != null) {
            releaseUsage(storeOrder.getKhuyenMaiShop());
        }
    }

    private void releaseUsage(Promotion promotion) {
        promotionRepository.findByIdForUpdate(promotion.getMaKM()).ifPresent(locked -> {
            locked.setDaSuDung(Math.max(0, used(locked) - 1));
            promotionRepository.save(locked);
        });
    }

    private Promotion findByCode(String code, VoucherType type) {
        if (code == null || code.isBlank()) {
            throw new BusinessException("Vui lòng nhập mã voucher");
        }
        return promotionRepository.findByCodeIgnoreCase(code.trim())
                .filter(p -> type.name().equals(p.getLoaiVoucher()))
                .orElseThrow(() -> new BusinessException(UNKNOWN_VOUCHER));
    }

    private Promotion lockByCode(String code, VoucherType type) {
        if (code == null || code.isBlank()) {
            throw new BusinessException("Vui lòng nhập mã voucher");
        }
        return promotionRepository.findByCodeForUpdate(code.trim())
                .filter(p -> type.name().equals(p.getLoaiVoucher()))
                .orElseThrow(() -> new BusinessException(UNKNOWN_VOUCHER));
    }

    private void requireStore(Promotion promotion, Integer maCH) {
        if (promotion.getCuaHang() == null || !Objects.equals(promotion.getCuaHang().getMaCH(), maCH)) {
            throw new BusinessException(WRONG_STORE);
        }
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
                .maCH(promotion.getCuaHang() == null ? null : promotion.getCuaHang().getMaCH())
                .tenCuaHang(promotion.getCuaHang() == null ? null : promotion.getCuaHang().getTenCuaHang())
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

    private List<CartItem> selectedItems(Integer maND, List<Integer> maCTGioHangs) {
        Cart cart = cartRepository.findByMaND(maND)
                .orElseThrow(() -> new BusinessException("Giỏ hàng đang trống"));

        Set<Integer> selected = new HashSet<>(maCTGioHangs);
        List<CartItem> items = cartItemRepository.findAllByGioHang(cart.getMaGioHang()).stream()
                .filter(ct -> selected.contains(ct.getMaCTGioHang()))
                .toList();

        if (items.isEmpty()) {
            throw new BusinessException("Sản phẩm được chọn không còn trong giỏ hàng");
        }
        return items;
    }

    private BigDecimal subtotal(List<CartItem> items) {
        return items.stream().map(VoucherService::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Map<Integer, BigDecimal> subtotalsByStore(List<CartItem> items) {
        Map<Integer, BigDecimal> result = new LinkedHashMap<>();
        for (CartItem item : items) {
            result.merge(item.getSach().getCuaHang().getMaCH(), lineTotal(item), BigDecimal::add);
        }
        return result;
    }

    private static BigDecimal lineTotal(CartItem item) {
        return nz(item.getSach().getGiaBanCu()).multiply(BigDecimal.valueOf(item.getSoLuong()));
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
