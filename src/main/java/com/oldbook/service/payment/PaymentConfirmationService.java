package com.oldbook.service.payment;

import com.oldbook.constant.payment.ReconciliationResult;
import com.oldbook.constant.payment.PaymentSessionStatus;
import com.oldbook.dto.order.OrderResponse;
import com.oldbook.entity.payment.BankTransaction;
import com.oldbook.entity.payment.PaymentSession;
import com.oldbook.entity.payment.Payment;
import com.oldbook.exception.common.BusinessException;
import com.oldbook.repository.order.OrderRepository;
import com.oldbook.repository.payment.BankTransactionRepository;
import com.oldbook.repository.payment.PaymentSessionRepository;
import com.oldbook.repository.payment.PaymentRepository;
import com.oldbook.service.notification.BusinessNotificationService;
import com.oldbook.service.order.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Xử lý một giao dịch ngân hàng do Casso báo về: ghi sổ, tìm phiên thanh toán, tạo đơn hàng.
 *
 *   Tình huống                                          | Kết quả
 *   Casso gửi lại giao dịch đã xử lý xong               | bỏ qua
 *   không phải tiền vào, hoặc nội dung không có mã phiên | KHONG_KHOP, chỉ ghi sổ
 *   đủ tiền, tạo được đơn                               | KHOP, đơn CHUYEN_KHOAN đã thanh toán
 *   thiếu tiền, hết hàng, giá hoặc giỏ đã đổi            | CAN_HOAN_TIEN, lập khoản hoàn đúng số đã nhận
 *   phiên đã được trả bằng một giao dịch khác            | CAN_HOAN_TIEN, lập khoản hoàn
 *
 * Phiên đã hết hạn hoặc khách đã hủy mà tiền vẫn về thì vẫn thử tạo đơn: còn hàng là khách có đơn.
 *
 * Lớp này không tự mở giao dịch database bao ngoài: việc tạo đơn chạy trong một giao dịch riêng để khi
 * nó thất bại và bị hủy, bước chuyển sang hoàn tiền vẫn ghi được ở giao dịch kế tiếp.
 */
@Slf4j
@Service
public class PaymentConfirmationService {

    public static final String CONG_CASSO = "CASSO";
    public static final String LOAI_GD_KHACH_CHUYEN_KHOAN = "KHACH_CHUYEN_KHOAN";

    private static final String TX_SUCCESS = "THANH_CONG";
    private static final Pattern MAU_MA_THANH_TOAN = Pattern.compile(
            PaymentSessionService.TIEN_TO_MA
                    + "[" + PaymentSessionService.BANG_KY_TU_MA + "]{" + PaymentSessionService.SO_KY_TU_MA + "}"
    );

    private final BankTransactionRepository bankTransactionRepository;
    private final PaymentSessionRepository paymentSessionRepository;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentSessionService paymentSessionService;
    private final OrderService orderService;
    private final TransferService transferService;
    private final BusinessNotificationService businessNotificationService;
    // Luôn mở giao dịch database mới, kể cả khi nơi gọi đang có sẵn một giao dịch.
    private final TransactionTemplate transactionTemplate;

    public PaymentConfirmationService(
            BankTransactionRepository bankTransactionRepository,
            PaymentSessionRepository paymentSessionRepository,
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            PaymentSessionService paymentSessionService,
            OrderService orderService,
            TransferService transferService,
            BusinessNotificationService businessNotificationService,
            PlatformTransactionManager transactionManager
    ) {
        this.bankTransactionRepository = bankTransactionRepository;
        this.paymentSessionRepository = paymentSessionRepository;
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.paymentSessionService = paymentSessionService;
        this.orderService = orderService;
        this.transferService = transferService;
        this.businessNotificationService = businessNotificationService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** Một giao dịch trên tài khoản của trụ sở, đã tách khỏi định dạng riêng của Casso. */
    public record IncomingTransaction(
            String maGiaoDichCong,
            String maThamChieu,
            BigDecimal soTien,
            String noiDung,
            LocalDateTime thoiGian,
            String soTaiKhoanNhan,
            String tenNguoiChuyen,
            String soTaiKhoanChuyen,
            String maNganHangChuyen
    ) {
    }

    /** Trả về null khi giao dịch này đã được xử lý xong từ trước. */
    public ReconciliationResult process(IncomingTransaction giaoDich) {
        Integer maGiaoDich = ghiSo(giaoDich);
        if (maGiaoDich == null) {
            return null;
        }

        if (giaoDich.soTien() == null || giaoDich.soTien().signum() <= 0) {
            return ketThuc(maGiaoDich, null, ReconciliationResult.KHONG_KHOP, "Không phải giao dịch tiền vào");
        }

        Integer maPhien = timPhien(giaoDich.noiDung()).orElse(null);
        if (maPhien == null) {
            return ketThuc(maGiaoDich, null, ReconciliationResult.KHONG_KHOP, "Nội dung không chứa mã thanh toán nào");
        }

        try {
            String ghiChu = transactionTemplate.execute(status -> taoDon(maPhien, giaoDich));
            return ketThuc(maGiaoDich, maPhien, ReconciliationResult.KHOP, ghiChu);
        } catch (BusinessException ex) {
            // Giao dịch tạo đơn đã bị hủy trọn vẹn; ghi nhận việc phải hoàn tiền ở một giao dịch mới.
            transactionTemplate.executeWithoutResult(status -> chuyenSangHoanTien(maPhien, maGiaoDich, ex.getMessage()));
            return ketThuc(maGiaoDich, maPhien, ReconciliationResult.CAN_HOAN_TIEN, ex.getMessage());
        }
    }

    /**
     * Ghi giao dịch vào sổ. Mã Casso là duy nhất nên cùng một giao dịch chỉ được nhận một lần;
     * riêng dòng còn DANG_XU_LY (lần trước lỗi giữa chừng) thì cho xử lý tiếp khi Casso gửi lại.
     */
    private Integer ghiSo(IncomingTransaction giaoDich) {
        try {
            return transactionTemplate.execute(status -> {
                Optional<BankTransaction> daCo = bankTransactionRepository.findByMaGiaoDichCong(giaoDich.maGiaoDichCong());
                if (daCo.isPresent()) {
                    return ReconciliationResult.DANG_XU_LY.name().equals(daCo.get().getKetQua())
                            ? daCo.get().getMaGiaoDich()
                            : null;
                }
                return bankTransactionRepository.save(BankTransaction.builder()
                        .maGiaoDichCong(giaoDich.maGiaoDichCong())
                        .maThamChieu(cut(giaoDich.maThamChieu(), 100))
                        .soTien(giaoDich.soTien() == null ? BigDecimal.ZERO : giaoDich.soTien())
                        .noiDung(cut(giaoDich.noiDung(), 1000))
                        .thoiGianGiaoDich(giaoDich.thoiGian())
                        .soTaiKhoanNhan(cut(giaoDich.soTaiKhoanNhan(), 50))
                        .tenNguoiChuyen(cut(giaoDich.tenNguoiChuyen(), 255))
                        .soTaiKhoanChuyen(cut(giaoDich.soTaiKhoanChuyen(), 50))
                        .maNganHangChuyen(cut(giaoDich.maNganHangChuyen(), 20))
                        .ketQua(ReconciliationResult.DANG_XU_LY.name())
                        .build()).getMaGiaoDich();
            });
        } catch (DataIntegrityViolationException ex) {
            // Hai yêu cầu cùng một giao dịch đến đồng thời: yêu cầu kia đã ghi sổ và sẽ xử lý.
            return null;
        }
    }

    private String taoDon(Integer maPhien, IncomingTransaction giaoDich) {
        PaymentSession phien = paymentSessionRepository.findByIdForUpdate(maPhien)
                .orElseThrow(() -> new BusinessException("Phiên thanh toán không còn tồn tại"));

        if (PaymentSessionStatus.DA_THANH_TOAN.name().equals(phien.getTrangThai())) {
            // Chính giao dịch này đã tạo đơn ở lần gửi trước: không làm gì thêm.
            if (paymentRepository.existsByMaGiaoDichCongAndLoaiGiaoDich(
                    giaoDich.maGiaoDichCong(), LOAI_GD_KHACH_CHUYEN_KHOAN)) {
                return null;
            }
            throw new BusinessException("Mã thanh toán này đã được trả bằng một giao dịch khác");
        }
        if (PaymentSessionStatus.CAN_HOAN_TIEN.name().equals(phien.getTrangThai())) {
            throw new BusinessException("Phiên thanh toán đã chuyển sang hoàn tiền");
        }
        if (giaoDich.soTien().compareTo(phien.getSoTien()) < 0) {
            throw new BusinessException("Số tiền chuyển khoản ít hơn số tiền cần thanh toán");
        }

        // Nhả hàng đang giữ rồi đặt hàng theo đúng luồng thường. Cùng một giao dịch database và sách đang bị
        // khóa, nên không ai chen vào lấy được sách giữa hai bước.
        paymentSessionService.releaseStock(phien);
        Integer maND = phien.getNguoiMua().getMaND();
        OrderResponse donHang = orderService.createPaidOrder(maND, paymentSessionService.orderRequestOf(phien));

        if (donHang.getTongThanhToan().compareTo(phien.getSoTien()) != 0) {
            throw new BusinessException("Giá sách hoặc phí vận chuyển đã thay đổi sau khi tạo mã thanh toán");
        }

        LocalDateTime now = LocalDateTime.now();
        paymentRepository.save(Payment.builder()
                .donHang(orderRepository.getReferenceById(donHang.getMaDH()))
                .congThanhToan(CONG_CASSO)
                .loaiGiaoDich(LOAI_GD_KHACH_CHUYEN_KHOAN)
                .maGiaoDichCong(giaoDich.maGiaoDichCong())
                .soTien(giaoDich.soTien())
                .trangThaiGiaoDich(TX_SUCCESS)
                .thoiGianTT(now)
                .build());

        phien.setTrangThai(PaymentSessionStatus.DA_THANH_TOAN.name());
        phien.setDonHang(orderRepository.getReferenceById(donHang.getMaDH()));
        phien.setSoTienNhan(giaoDich.soTien());
        paymentSessionRepository.save(phien);
        paymentSessionService.notifyStatusChanged(phien);

        businessNotificationService.paymentSucceeded(phien.getNguoiMua(), donHang.getMaDH(), giaoDich.soTien());

        BigDecimal thua = giaoDich.soTien().subtract(phien.getSoTien());
        return thua.signum() > 0 ? "Khách chuyển thừa " + thua.toPlainString() + " đ" : null;
    }

    private void chuyenSangHoanTien(Integer maPhien, Integer maGiaoDich, String lyDo) {
        PaymentSession phien = paymentSessionRepository.findByIdForUpdate(maPhien)
                .orElseThrow(() -> new BusinessException("Phiên thanh toán không còn tồn tại"));
        BankTransaction giaoDich = bankTransactionRepository.findById(maGiaoDich)
                .orElseThrow(() -> new BusinessException("Giao dịch không còn tồn tại"));

        // Phiên đã có đơn (khách trả hai lần) thì giữ nguyên đơn, chỉ hoàn giao dịch thừa.
        if (!PaymentSessionStatus.DA_THANH_TOAN.name().equals(phien.getTrangThai())) {
            paymentSessionService.releaseStock(phien);
            phien.setTrangThai(PaymentSessionStatus.CAN_HOAN_TIEN.name());
            phien.setSoTienNhan(giaoDich.getSoTien());
            paymentSessionRepository.save(phien);
            paymentSessionService.notifyStatusChanged(phien);
        }

        transferService.createRefundForSession(phien, giaoDich, lyDo);
    }

    private ReconciliationResult ketThuc(Integer maGiaoDich, Integer maPhien, ReconciliationResult ketQua, String ghiChu) {
        transactionTemplate.executeWithoutResult(status -> {
            BankTransaction giaoDich = bankTransactionRepository.findById(maGiaoDich).orElseThrow();
            giaoDich.setKetQua(ketQua.name());
            giaoDich.setGhiChu(cut(ghiChu, 500));
            if (maPhien != null) {
                giaoDich.setPhien(paymentSessionRepository.getReferenceById(maPhien));
            }
            bankTransactionRepository.save(giaoDich);
            if (ketQua == ReconciliationResult.KHONG_KHOP || ketQua == ReconciliationResult.CAN_HOAN_TIEN) {
                businessNotificationService.bankTransactionMismatch(giaoDich.getSoTien(), ketQua.name(), ghiChu);
            }
        });
        log.info("Giao dịch ngân hàng {}: {}{}", maGiaoDich, ketQua, ghiChu == null ? "" : " (" + ghiChu + ")");
        return ketQua;
    }

    // Ngân hàng hay chèn thêm chữ, dấu chấm, khoảng trắng quanh nội dung, nên bỏ hết ký tự lạ rồi mới dò mã.
    private Optional<Integer> timPhien(String noiDung) {
        if (noiDung == null) {
            return Optional.empty();
        }
        String normalize = noiDung.toUpperCase().replaceAll("[^A-Z0-9]", "");
        Matcher matcher = MAU_MA_THANH_TOAN.matcher(normalize);

        int from = 0;
        while (matcher.find(from)) {
            Optional<PaymentSession> phien = paymentSessionRepository.findByMaThanhToan(matcher.group());
            if (phien.isPresent()) {
                return phien.map(PaymentSession::getMaPhien);
            }
            // Dò tiếp từ ký tự kế bên: mã thật có thể bắt đầu ngay trong một chuỗi trông giống mã.
            from = matcher.start() + 1;
        }
        return Optional.empty();
    }

    private static String cut(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
