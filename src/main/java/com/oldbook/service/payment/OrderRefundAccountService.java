package com.oldbook.service.payment;

import com.oldbook.constant.order.PaymentMethod;
import com.oldbook.constant.payment.ReconciliationResult;
import com.oldbook.dto.payment.BankAccountRequest;
import com.oldbook.entity.order.Order;
import com.oldbook.entity.payment.PaymentSession;
import com.oldbook.exception.common.BusinessException;
import com.oldbook.repository.order.OrderRepository;
import com.oldbook.repository.payment.BankTransactionRepository;
import com.oldbook.repository.payment.PaymentSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tài khoản nhận hoàn tiền khách chọn lúc hủy đơn chuyển khoản.
 *
 * Lưu ở phiên thanh toán của đơn (không đụng tới yêu cầu hủy đơn), nên dùng được cho cả "Hủy đơn" (hủy ngay) lẫn
 * "Gửi yêu cầu hủy" (lúc đó chưa có yêu cầu hoàn tiền). Khi lập khoản hoàn, TransferService đọc lại các cột này.
 */
@Service
@RequiredArgsConstructor
public class OrderRefundAccountService {

    private final OrderRepository orderRepository;
    private final PaymentSessionRepository paymentSessionRepository;
    private final BankTransactionRepository bankTransactionRepository;

    /** Thông tin cho hộp thoại hủy đơn: tài khoản hệ thống đã biết và tài khoản hoàn khách đã chọn (nếu có). */
    public record ThongTin(
            boolean chuyenKhoan,
            // Tài khoản người chuyển trong giao dịch ngân hàng, số tài khoản đã che.
            String maNganHangDaChuyen,
            String soTaiKhoanDaChuyenRutGon,
            String tenNguoiChuyen,
            // Tài khoản hoàn khách đã chọn lúc hủy đơn.
            String maNganHangUuTien,
            String soTaiKhoanUuTien,
            String tenChuTaiKhoanUuTien
    ) {
    }

    @Transactional(readOnly = true)
    public ThongTin thongTin(Integer maND, Integer maDH) {
        Order donHang = donCuaKhach(maND, maDH);
        if (!PaymentMethod.isPrepaid(donHang.getPhuongThucTT())) {
            return new ThongTin(false, null, null, null, null, null, null);
        }
        PaymentSession phien = paymentSessionRepository.findFirstByDonHang_MaDH(maDH).orElse(null);
        if (phien == null) {
            return new ThongTin(true, null, null, null, null, null, null);
        }
        var giaoDich = bankTransactionRepository
                .findFirstByPhien_MaPhienAndKetQuaOrderByMaGiaoDichDesc(phien.getMaPhien(), ReconciliationResult.KHOP.name())
                .filter(g -> g.getSoTaiKhoanChuyen() != null && !g.getSoTaiKhoanChuyen().isBlank())
                .orElse(null);
        return new ThongTin(true,
                giaoDich == null ? null : giaoDich.getMaNganHangChuyen(),
                giaoDich == null ? null : rutGon(giaoDich.getSoTaiKhoanChuyen()),
                giaoDich == null ? null : giaoDich.getTenNguoiChuyen(),
                phien.getHoanMaNganHang(), phien.getHoanSoTaiKhoan(), phien.getHoanTenChuTaiKhoan());
    }

    @Transactional
    public ThongTin persist(Integer maND, Integer maDH, BankAccountRequest request) {
        Order donHang = donCuaKhach(maND, maDH);
        if (!PaymentMethod.isPrepaid(donHang.getPhuongThucTT())) {
            throw new BusinessException("Chỉ đơn chuyển khoản mới có khoản hoàn tiền");
        }
        PaymentSession phien = paymentSessionRepository.findFirstByDonHang_MaDH(maDH)
                .flatMap(p -> paymentSessionRepository.findByIdForUpdate(p.getMaPhien()))
                .orElseThrow(() -> new BusinessException("Không tìm thấy lần thanh toán của đơn này"));
        phien.setHoanMaNganHang(request.maNganHang().trim());
        phien.setHoanSoTaiKhoan(request.soTaiKhoan().trim());
        phien.setHoanTenChuTaiKhoan(request.tenChuTaiKhoan().trim());
        paymentSessionRepository.save(phien);
        return thongTin(maND, maDH);
    }

    private Order donCuaKhach(Integer maND, Integer maDH) {
        return orderRepository.findById(maDH)
                .filter(d -> d.getNguoiMua() != null && maND.equals(d.getNguoiMua().getMaND()))
                .orElseThrow(() -> new BusinessException("Đơn hàng không tồn tại"));
    }

    /** 1234567890 -> ***7890 */
    static String rutGon(String soTaiKhoan) {
        String so = soTaiKhoan.trim();
        return "***" + (so.length() <= 4 ? so : so.substring(so.length() - 4));
    }
}
