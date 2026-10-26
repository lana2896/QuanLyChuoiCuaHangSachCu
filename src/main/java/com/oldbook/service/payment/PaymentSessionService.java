package com.oldbook.service.payment;

import com.oldbook.config.payment.PaymentProperties;
import com.oldbook.constant.order.PaymentMethod;
import com.oldbook.constant.payment.PaymentSessionStatus;
import com.oldbook.dto.order.CreateOrderRequest;
import com.oldbook.dto.order.OrderPreview;
import com.oldbook.dto.payment.PaymentConfigResponse;
import com.oldbook.dto.payment.PaymentSessionResponse;
import com.oldbook.entity.catalog.Book;
import com.oldbook.entity.payment.PaymentSession;
import com.oldbook.exception.common.BusinessException;
import com.oldbook.repository.catalog.BookRepository;
import com.oldbook.repository.identity.UserRepository;
import com.oldbook.repository.payment.PaymentSessionRepository;
import com.oldbook.service.order.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Vòng đời của một phiên thanh toán chuyển khoản: tạo phiên và giữ hàng, khách hủy, hết hạn.
 * Việc tiền về nằm ở PaymentConfirmationService.
 *
 * Giữ hàng nghĩa là trừ thẳng vào tồn kho của sách; trả kho là cộng lại đúng số đó.
 * Mọi thay đổi trạng thái đều khóa dòng phiên trước, nên hết hạn, hủy và tiền về không chạy chồng lên nhau.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentSessionService {

    public static final String TIEN_TO_MA = "OB";
    public static final int SO_KY_TU_MA = 8;
    // Bỏ 0, 1, I, O để khách tự gõ nội dung chuyển khoản cũng khó nhầm.
    public static final String BANG_KY_TU_MA = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";

    private static final String CHO_THANH_TOAN = PaymentSessionStatus.CHO_THANH_TOAN.name();
    private static final TypeReference<List<OrderPreview.Line>> KIEU_GIU_HANG = new TypeReference<>() {
    };

    private final PaymentProperties properties;
    private final PaymentSessionRepository paymentSessionRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final OrderService orderService;
    private final VietQrLinkBuilder vietQrLinkBuilder;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    private final SecureRandom random = new SecureRandom();

    public PaymentConfigResponse getConfig() {
        return new PaymentConfigResponse(properties.isEnabled(), properties.getSessionMinutes());
    }

    @Transactional
    public PaymentSessionResponse createSession(Integer maND, CreateOrderRequest request) {
        if (!properties.isEnabled()) {
            throw new BusinessException("Sàn chưa bật thanh toán chuyển khoản");
        }

        // Khách bấm thanh toán lại: phiên chờ cũ phải nhả hàng trước, nếu không chính họ sẽ bị báo hết hàng.
        for (Integer maPhienCu : paymentSessionRepository.findIdsByNguoiMuaAndTrangThai(maND, CHO_THANH_TOAN)) {
            ketThucNeuDangCho(maPhienCu, PaymentSessionStatus.DA_HUY);
        }

        request.setPhuongThucTT(PaymentMethod.CHUYEN_KHOAN.name());
        OrderPreview preview = orderService.previewOrder(maND, request);

        for (OrderPreview.Line line : preview.lines()) {
            doiTonKho(line.maSach(), -line.soLuong());
        }

        PaymentSession phien = paymentSessionRepository.save(PaymentSession.builder()
                .nguoiMua(userRepository.getReferenceById(maND))
                .maThanhToan(taoMaThanhToan())
                .soTien(preview.tongThanhToan())
                .trangThai(CHO_THANH_TOAN)
                .hetHanLuc(LocalDateTime.now().plusMinutes(properties.getSessionMinutes()))
                .yeuCauJson(objectMapper.writeValueAsString(request))
                .giuHangJson(objectMapper.writeValueAsString(preview.lines()))
                .daTraKho(false)
                .build());

        return toResponse(phien);
    }

    @Transactional(readOnly = true)
    public PaymentSessionResponse getSession(Integer maND, Integer maPhien) {
        return toResponse(phienCuaKhach(paymentSessionRepository.findById(maPhien).orElse(null), maND));
    }

    @Transactional
    public PaymentSessionResponse cancelSession(Integer maND, Integer maPhien) {
        PaymentSession phien = phienCuaKhach(paymentSessionRepository.findByIdForUpdate(maPhien).orElse(null), maND);

        if (CHO_THANH_TOAN.equals(phien.getTrangThai())) {
            releaseStock(phien);
            phien.setTrangThai(PaymentSessionStatus.DA_HUY.name());
            paymentSessionRepository.save(phien);
        }

        return toResponse(phien);
    }

    /** Cộng lại tồn kho cho các sách phiên đang giữ. Gọi nhiều lần cũng chỉ trả một lần. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void releaseStock(PaymentSession phien) {
        if (phien.isDaTraKho()) {
            return;
        }
        for (OrderPreview.Line line : objectMapper.readValue(phien.getGiuHangJson(), KIEU_GIU_HANG)) {
            doiTonKho(line.maSach(), line.soLuong());
        }
        phien.setDaTraKho(true);
        paymentSessionRepository.save(phien);
    }

    /** Yêu cầu đặt hàng đã chụp lại lúc tạo phiên. */
    public CreateOrderRequest orderRequestOf(PaymentSession phien) {
        return objectMapper.readValue(phien.getYeuCauJson(), CreateOrderRequest.class);
    }

    /**
     * Dọn các phiên quá hạn. Mỗi phiên một giao dịch riêng: một phiên lỗi không chặn các phiên còn lại.
     * Tiền về sau khi phiên đã hết hạn vẫn được xử lý ở PaymentConfirmationService.
     */
    @Scheduled(fixedDelayString = "${payment.expiry-check-ms:30000}")
    public void expireSessions() {
        List<Integer> quaHan = paymentSessionRepository.findIdsHetHan(CHO_THANH_TOAN, LocalDateTime.now());

        for (Integer maPhien : quaHan) {
            try {
                transactionTemplate.executeWithoutResult(status -> {
                    PaymentSession phien = paymentSessionRepository.findByIdForUpdate(maPhien).orElse(null);
                    // Kiểm tra lại sau khi khóa: tiền có thể vừa về trong lúc chờ.
                    if (phien != null
                            && CHO_THANH_TOAN.equals(phien.getTrangThai())
                            && phien.getHetHanLuc().isBefore(LocalDateTime.now())) {
                        releaseStock(phien);
                        phien.setTrangThai(PaymentSessionStatus.HET_HAN.name());
                        paymentSessionRepository.save(phien);
                    }
                });
            } catch (RuntimeException ex) {
                log.warn("Không dọn được phiên thanh toán {}: {}", maPhien, ex.getMessage());
            }
        }
    }

    public PaymentSessionResponse toResponse(PaymentSession phien) {
        PaymentProperties.Bank bank = properties.getBank();
        long conLai = Math.max(0, Duration.between(LocalDateTime.now(), phien.getHetHanLuc()).getSeconds());

        return new PaymentSessionResponse(
                phien.getMaPhien(),
                phien.getMaThanhToan(),
                phien.getSoTien(),
                phien.getTrangThai(),
                phien.getHetHanLuc(),
                conLai,
                vietQrLinkBuilder.build(
                        bank.getBin(), bank.getAccountNo(), bank.getAccountName(),
                        phien.getSoTien(), phien.getMaThanhToan()
                ),
                bank.getBin(),
                bank.getAccountNo(),
                bank.getAccountName(),
                phien.getDonHang() == null ? null : phien.getDonHang().getMaDH()
        );
    }

    private void ketThucNeuDangCho(Integer maPhien, PaymentSessionStatus trangThaiMoi) {
        PaymentSession phien = paymentSessionRepository.findByIdForUpdate(maPhien).orElse(null);
        if (phien == null || !CHO_THANH_TOAN.equals(phien.getTrangThai())) {
            return;
        }
        releaseStock(phien);
        phien.setTrangThai(trangThaiMoi.name());
        paymentSessionRepository.save(phien);
    }

    private void doiTonKho(Integer maSach, int chenhLech) {
        Book sach = bookRepository.findByIdForUpdate(maSach)
                .orElseThrow(() -> new BusinessException("Sách không tồn tại: " + maSach));
        int ton = sach.getSoLuongTon() == null ? 0 : sach.getSoLuongTon();
        sach.setSoLuongTon(ton + chenhLech);
        bookRepository.save(sach);
    }

    // Không trả lời khác nhau giữa "không tồn tại" và "của người khác", để không lộ mã phiên của ai.
    private static PaymentSession phienCuaKhach(PaymentSession phien, Integer maND) {
        if (phien == null || !maND.equals(phien.getNguoiMua().getMaND())) {
            throw new BusinessException("Phiên thanh toán không tồn tại");
        }
        return phien;
    }

    private String taoMaThanhToan() {
        while (true) {
            StringBuilder ma = new StringBuilder(TIEN_TO_MA);
            for (int i = 0; i < SO_KY_TU_MA; i++) {
                ma.append(BANG_KY_TU_MA.charAt(random.nextInt(BANG_KY_TU_MA.length())));
            }
            if (!paymentSessionRepository.existsByMaThanhToan(ma.toString())) {
                return ma.toString();
            }
        }
    }
}
