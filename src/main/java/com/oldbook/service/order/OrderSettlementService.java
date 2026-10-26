package com.oldbook.service.order;

import com.oldbook.constant.order.OrderStatus;
import com.oldbook.constant.order.PaymentMethod;
import com.oldbook.constant.order.PaymentStatus;
import com.oldbook.entity.catalog.Book;
import com.oldbook.entity.order.OrderItem;
import com.oldbook.entity.order.Order;
import com.oldbook.entity.order.StoreOrder;
import com.oldbook.entity.payment.CommissionConfig;
import com.oldbook.entity.payment.Payment;
import com.oldbook.exception.common.BusinessException;
import com.oldbook.repository.catalog.BookRepository;
import com.oldbook.repository.order.OrderItemRepository;
import com.oldbook.repository.order.StoreOrderRepository;
import com.oldbook.repository.order.OrderRepository;
import com.oldbook.repository.payment.CommissionConfigRepository;
import com.oldbook.repository.payment.PaymentRepository;
import com.oldbook.service.payment.VoucherService;
import com.oldbook.service.payment.TransferService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderSettlementService {

    public static final String TYPE_COD_COLLECTION = "THU_HO_COD";
    public static final String TYPE_COMMISSION = "HOA_HONG";
    public static final String TYPE_SHOP_REVENUE = "DOANH_THU_SHOP";
    public static final String TYPE_RETURN_SHIPPING = "PHI_SHIP_HOAN_HANG";

    private static final String GATEWAY_COD = "COD";
    private static final String TX_SUCCESS = "THANH_CONG";
    private static final String COMMISSION_ACTIVE = "HOAT_DONG";
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final OrderItemRepository orderItemRepository;
    private final CommissionConfigRepository commissionConfigRepository;
    private final PaymentRepository paymentRepository;
    private final BookRepository bookRepository;
    private final OrderRepository orderRepository;
    private final StoreOrderRepository storeOrderRepository;
    private final EntityManager entityManager;
    private final VoucherService voucherService;
    private final TransferService transferService;

    /** Tỷ lệ chiết khấu (%) áp dụng khi danh mục chưa có cấu hình chiết khấu đang hiệu lực. */
    @Value("${marketplace.commission.default-rate:5.00}")
    private BigDecimal defaultCommissionRate;

    @Transactional(propagation = Propagation.MANDATORY)
    public void settleDelivered(StoreOrder order, String maVanDon) {
        if (order.getNgayGiao() != null) {
            return; // đã chốt, tránh ghi nhận doanh thu hai lần
        }

        LocalDateTime now = LocalDateTime.now();
        List<OrderItem> lines = orderItemRepository
                .findAllByDonHangCuaHang_MaDHCHOrderByMaCTDHAsc(order.getMaDHCH());

        Map<Integer, BigDecimal> rateByCategory = new HashMap<>();
        BigDecimal commission = BigDecimal.ZERO;
        BigDecimal giamGiaShop = nz(order.getGiamGiaShop());
        BigDecimal tienHangCH = nz(order.getTienHangCH());

        for (OrderItem line : lines) {
            Integer maDM = line.getSach().getDanhMuc().getMaDM();
            BigDecimal rate = rateByCategory.computeIfAbsent(maDM, id -> commissionRate(id, now));
            BigDecimal base = nz(line.getThanhTien());
            if (giamGiaShop.signum() > 0 && tienHangCH.signum() > 0) {
                base = base.subtract(giamGiaShop.multiply(base).divide(tienHangCH, 2, RoundingMode.HALF_UP))
                        .max(BigDecimal.ZERO);
            }
            BigDecimal tien = base.multiply(rate).divide(HUNDRED, 2, RoundingMode.HALF_UP);

            line.setTyLeChietKhau(rate);
            line.setTienChietKhau(tien);
            commission = commission.add(tien);
        }
        orderItemRepository.saveAll(lines);

        BigDecimal doanhThu = nz(order.getTienHangCH())
                .subtract(nz(order.getGiamGiaShop()))
                .subtract(commission)
                .max(BigDecimal.ZERO);

        order.setCommissionTien(commission);
        order.setDoanhThuShop(doanhThu);
        order.setTienHoanTra(BigDecimal.ZERO);
        order.setTrangThaiThanhToanShop(PaymentStatus.DA_THANH_TOAN.name());
        order.setNgayGiao(now);
        storeOrderRepository.save(order);

        boolean prepaid = isPrepaid(order);
        if (!prepaid) {
            // COD: đơn vị vận chuyển thu đủ tiền hàng + phí ship từ khách khi giao thành công.
            recordTransaction(order, TYPE_COD_COLLECTION, order.getTongTienShopOrder(), maVanDon, now);
        }
        recordTransaction(order, TYPE_COMMISSION, commission, maVanDon, now);
        recordTransaction(order, TYPE_SHOP_REVENUE, doanhThu, maVanDon, now);
        // Chuyển khoản và COD: tiền của đơn đã về trụ sở, nên trụ sở phải chi phần doanh thu cho cửa hàng.
        transferService.createStorePayout(order);

        refreshParentPaymentStatus(order.getDonHang().getMaDH());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void settleReturned(StoreOrder order) {
        if (order.getNgayHoanTra() != null) {
            return; // đã hoàn kho, tránh cộng tồn hai lần
        }

        List<OrderItem> lines = orderItemRepository
                .findAllByDonHangCuaHang_MaDHCHOrderByMaCTDHAsc(order.getMaDHCH());

        for (OrderItem line : lines) {
            Book sach = bookRepository.findByIdForUpdate(line.getSach().getMaSach())
                    .orElseThrow(() -> new BusinessException("Sách không tồn tại khi hoàn kho"));
            sach.setSoLuongTon(nz(sach.getSoLuongTon()) + line.getSoLuong());
            bookRepository.save(sach);

            line.setTyLeChietKhau(BigDecimal.ZERO);
            line.setTienChietKhau(BigDecimal.ZERO);
        }
        orderItemRepository.saveAll(lines);

        order.setCommissionTien(BigDecimal.ZERO);
        order.setDoanhThuShop(BigDecimal.ZERO);
        LocalDateTime now = LocalDateTime.now();
        order.setTienHoanTra(isPrepaid(order)
                ? nz(order.getTongTienShopOrder()).subtract(nz(order.getPhiShipCH())).max(BigDecimal.ZERO)
                : BigDecimal.ZERO);
        order.setTrangThaiThanhToanShop(PaymentStatus.KHONG_THANH_TOAN.name());
        order.setNgayHoanTra(now);
        storeOrderRepository.save(order);
        if (isPrepaid(order)) {
            recordTransaction(order, TYPE_RETURN_SHIPPING, order.getPhiShipCH(), null, now);
            transferService.createRefundForOrder(order, "Giao hàng không thành công");
        }

        refreshParentPaymentStatus(order.getDonHang().getMaDH());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void settleCancelled(StoreOrder order, String huyBoi, String lyDo) {
        if (OrderStatus.DA_HUY.name().equals(order.getTrangThaiDonHang())) {
            return;
        }

        List<OrderItem> lines = orderItemRepository
                .findAllByDonHangCuaHang_MaDHCHOrderByMaCTDHAsc(order.getMaDHCH());

        for (OrderItem line : lines) {
            Book sach = bookRepository.findByIdForUpdate(line.getSach().getMaSach())
                    .orElseThrow(() -> new BusinessException("Sách không tồn tại khi hoàn kho"));
            sach.setSoLuongTon(nz(sach.getSoLuongTon()) + line.getSoLuong());
            bookRepository.save(sach);
        }

        order.setTrangThaiDonHang(OrderStatus.DA_HUY.name());
        order.setCommissionTien(BigDecimal.ZERO);
        order.setDoanhThuShop(BigDecimal.ZERO);
        order.setTienHoanTra(refundAmount(order));
        order.setTrangThaiThanhToanShop(PaymentStatus.KHONG_THANH_TOAN.name());
        order.setNgayHuy(LocalDateTime.now());
        order.setHuyBoi(huyBoi);
        order.setLyDoHuy(lyDo);
        storeOrderRepository.save(order);
        if (isPrepaid(order)) {
            transferService.createRefundForOrder(order, "Đơn hàng bị hủy");
        }

        refreshParentPaymentStatus(order.getDonHang().getMaDH());
        voucherService.restoreShopVoucher(order);
        voucherService.restoreIfFullyCancelled(order.getDonHang().getMaDH());
    }

    private BigDecimal commissionRate(Integer maDM, LocalDateTime now) {
        Optional<CommissionConfig> cfg = commissionConfigRepository
                .findEffective(maDM, COMMISSION_ACTIVE, now)
                .stream()
                .filter(c -> c.getTyLeChietKhau() != null)
                .findFirst();
        return cfg.map(CommissionConfig::getTyLeChietKhau).orElse(defaultCommissionRate);
    }

    private void recordTransaction(
            StoreOrder order, String loai, BigDecimal soTien, String maVanDon, LocalDateTime now
    ) {
        if (paymentRepository.existsByDonHangCuaHang_MaDHCHAndLoaiGiaoDich(order.getMaDHCH(), loai)) {
            return;
        }
        paymentRepository.save(Payment.builder()
                .donHang(order.getDonHang())
                .donHangCuaHang(order)
                .congThanhToan(GATEWAY_COD)
                .loaiGiaoDich(loai)
                .maGiaoDichCong(maVanDon)
                .soTien(nz(soTien))
                .trangThaiGiaoDich(TX_SUCCESS)
                .thoiGianTT(now)
                .build());
    }

    /**
     * Tính lại trạng thái thanh toán của đơn tổng từ các đơn con.
     * Khóa đơn tổng trước để hai đơn con của hai cửa hàng chốt cùng lúc không ghi đè kết quả của nhau.
     */
    private void refreshParentPaymentStatus(Integer maDH) {
        // Khóa bằng EntityManager để không phải sửa OrderRepository của module order.
        Order parent = Optional.ofNullable(entityManager.find(Order.class, maDH, LockModeType.PESSIMISTIC_WRITE))
                .orElseThrow(() -> new BusinessException("Đơn hàng không tồn tại"));

        List<StoreOrder> children = storeOrderRepository.findAllByDonHang_MaDHOrderByMaDHCHAsc(maDH);

        long delivered = children.stream()
                .filter(c -> OrderStatus.DA_GIAO.name().equals(c.getTrangThaiDonHang()))
                .count();
        boolean allFinal = children.stream().allMatch(c -> {
            OrderStatus st = OrderStatus.fromName(c.getTrangThaiDonHang());
            return st != null && st.isFinal();
        });

        PaymentStatus status;
        if (PaymentMethod.isPrepaid(parent.getPhuongThucTT())) {
            // Chuyển khoản: khách đã trả đủ từ đầu. Trạng thái chỉ đổi theo phần phải hoàn lại.
            long refunded = children.stream()
                    .filter(c -> OrderStatus.DA_HUY.name().equals(c.getTrangThaiDonHang())
                            || OrderStatus.DA_HOAN_TRA.name().equals(c.getTrangThaiDonHang()))
                    .count();
            if (refunded == 0) {
                status = PaymentStatus.DA_THANH_TOAN;
            } else {
                status = refunded == children.size()
                        ? PaymentStatus.KHONG_THANH_TOAN
                        : PaymentStatus.THANH_TOAN_MOT_PHAN;
            }
        } else if (delivered == children.size()) {
            status = PaymentStatus.DA_THANH_TOAN;
        } else if (delivered > 0) {
            status = PaymentStatus.THANH_TOAN_MOT_PHAN;
        } else {
            status = allFinal ? PaymentStatus.KHONG_THANH_TOAN : PaymentStatus.CHO_THANH_TOAN;
        }

        parent.setTrangThaiThanhToan(status.name());
        orderRepository.save(parent);
    }

    private static boolean isPrepaid(StoreOrder order) {
        return PaymentMethod.isPrepaid(order.getDonHang().getPhuongThucTT());
    }

    /** COD chưa thu tiền của khách nên không phải hoàn; chuyển khoản thì hoàn cả tiền hàng lẫn phí vận chuyển. */
    private static BigDecimal refundAmount(StoreOrder order) {
        return isPrepaid(order) ? nz(order.getTongTienShopOrder()) : BigDecimal.ZERO;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }
}
