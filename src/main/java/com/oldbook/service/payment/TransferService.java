package com.oldbook.service.payment;

import com.oldbook.constant.payment.ReconciliationResult;
import com.oldbook.constant.payment.TransferType;
import com.oldbook.constant.payment.TransferStatus;
import com.oldbook.dto.catalog.PageResponse;
import com.oldbook.dto.management.ManagementDto.GiaoDichTrang;
import com.oldbook.dto.management.ManagementDto.TomTatGiaoDich;
import com.oldbook.dto.payment.BankTransactionResponse;
import com.oldbook.dto.payment.BankAccountRequest;
import com.oldbook.dto.payment.TransferRequestResponse;
import com.oldbook.entity.catalog.Store;
import com.oldbook.entity.identity.User;
import com.oldbook.entity.order.StoreOrder;
import com.oldbook.entity.payment.BankTransaction;
import com.oldbook.entity.payment.PaymentSession;
import com.oldbook.entity.payment.Payment;
import com.oldbook.entity.payment.TransferRequest;
import com.oldbook.exception.common.BusinessException;
import com.oldbook.repository.identity.AccountRepository;
import com.oldbook.repository.payment.BankTransactionRepository;
import com.oldbook.repository.payment.PaymentSessionRepository;
import com.oldbook.repository.payment.PaymentRepository;
import com.oldbook.repository.payment.TransferRequestRepository;
import com.oldbook.service.management.OperationsChangedEvent;
import com.oldbook.service.notification.BusinessNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Các khoản tiền trụ sở phải chuyển đi cho đơn đã thanh toán trước: hoàn cho khách và chi cho cửa hàng.
 *
 *   Sự kiện                                  | Khoản lập ra                         | Trạng thái đầu
 *   đơn con bị hủy hoặc bị hoàn hàng         | hoàn cho khách, bằng tổng đơn con    | CHO_THONG_TIN
 *   tiền về nhưng không tạo được đơn         | hoàn cho khách, bằng số đã nhận      | CHO_THONG_TIN
 *   đơn con giao thành công                  | chi cho cửa hàng, bằng doanh thu shop | CHO_XU_LY
 *
 * Khoản hoàn chờ khách xác nhận tài khoản nhận rồi mới sang CHO_XU_LY. Hệ thống không tự chuyển tiền:
 * Quản lý chuyển khoản bằng tài khoản của trụ sở rồi đánh dấu DA_CHUYEN.
 */
@Service
@RequiredArgsConstructor
public class TransferService {

    public static final String LOAI_GD_HOAN_TIEN = "HOAN_TIEN";
    public static final String LOAI_GD_CHI_CUA_HANG = "CHI_CUA_HANG";

    private static final String CONG_CHUYEN_KHOAN = "CHUYEN_KHOAN";
    private static final String TX_SUCCESS = "THANH_CONG";
    private static final String HOAN_TIEN_KHACH = TransferType.HOAN_TIEN_KHACH.name();
    private static final String CHI_CUA_HANG = TransferType.CHI_CUA_HANG.name();

    private final TransferRequestRepository transferRequestRepository;
    private final PaymentSessionRepository paymentSessionRepository;
    private final BankTransactionRepository bankTransactionRepository;
    private final PaymentRepository paymentRepository;
    private final AccountRepository accountRepository;
    private final BusinessNotificationService businessNotificationService;
    private final VietQrLinkBuilder vietQrLinkBuilder;
    private final ApplicationEventPublisher eventPublisher;

    // ---------------------------------------------------------------
    // Lập khoản (gọi từ quyết toán và từ bước xác nhận tiền về)
    // ---------------------------------------------------------------

    @Transactional(propagation = Propagation.MANDATORY)
    public void createRefundForOrder(StoreOrder order, String lyDo) {
        if (transferRequestRepository.existsByLoaiAndDonHangCuaHang_MaDHCH(HOAN_TIEN_KHACH, order.getMaDHCH())) {
            return;
        }
        BigDecimal soTien = nz(order.getTongTienShopOrder());
        if (soTien.signum() <= 0) {
            return;
        }

        User khach = order.getDonHang().getNguoiMua();
        TransferRequest yeuCau = TransferRequest.builder()
                .loai(HOAN_TIEN_KHACH)
                .donHangCuaHang(order)
                .nguoiNhan(khach)
                .soTien(soTien)
                .trangThai(TransferStatus.CHO_THONG_TIN.name())
                .lyDo(cut(lyDo))
                .build();

        // Điền sẵn tài khoản nhận hoàn theo thứ tự ưu tiên:
        //   1. tài khoản hoàn khách chọn lúc hủy đơn;
        //   2. tài khoản người chuyển trong giao dịch ngân hàng (Casso gửi kèm).
        // Có tài khoản thì khoản hoàn vào thẳng CHO_XU_LY, Quản lý chuyển được ngay.
        PaymentSession phien = paymentSessionRepository.findFirstByDonHang_MaDH(order.getDonHang().getMaDH()).orElse(null);
        if (phien != null && coTaiKhoanUuTien(phien)) {
            yeuCau.setMaNganHang(phien.getHoanMaNganHang());
            yeuCau.setSoTaiKhoan(phien.getHoanSoTaiKhoan());
            yeuCau.setTenChuTaiKhoan(phien.getHoanTenChuTaiKhoan());
        } else if (phien != null) {
            bankTransactionRepository.findFirstByPhien_MaPhienAndKetQuaOrderByMaGiaoDichDesc(
                    phien.getMaPhien(), ReconciliationResult.KHOP.name())
                    .ifPresent(giaoDich -> dienSanTaiKhoan(yeuCau, giaoDich));
        }
        boolean daCoNoiNhan = yeuCau.getSoTaiKhoan() != null && !yeuCau.getSoTaiKhoan().isBlank();
        if (daCoNoiNhan) {
            yeuCau.setTrangThai(TransferStatus.CHO_XU_LY.name());
        }

        transferRequestRepository.save(yeuCau);

        String moTaDon = "Đơn #" + order.getMaDHCH() + " đã thanh toán trước bị hủy hoặc hoàn hàng.";
        if (daCoNoiNhan) {
            businessNotificationService.transferPending(true, soTien, moTaDon + " Đã có tài khoản nhận, có thể chuyển ngay.");
        } else {
            businessNotificationService.refundAccountNeeded(khach, soTien, moTaDon);
            businessNotificationService.transferPending(true, soTien, moTaDon + " Chờ khách nhập tài khoản nhận.");
        }
    }

    private static boolean coTaiKhoanUuTien(PaymentSession phien) {
        return phien.getHoanSoTaiKhoan() != null && !phien.getHoanSoTaiKhoan().isBlank();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void createRefundForSession(PaymentSession phien, BankTransaction giaoDich, String lyDo) {
        if (transferRequestRepository.existsByGiaoDichNganHang_MaGiaoDich(giaoDich.getMaGiaoDich())) {
            return;
        }

        TransferRequest yeuCau = TransferRequest.builder()
                .loai(HOAN_TIEN_KHACH)
                .phien(phien)
                .giaoDichNganHang(giaoDich)
                .nguoiNhan(phien.getNguoiMua())
                .soTien(giaoDich.getSoTien())
                .trangThai(TransferStatus.CHO_THONG_TIN.name())
                .lyDo(cut(lyDo))
                .build();
        dienSanTaiKhoan(yeuCau, giaoDich);
        transferRequestRepository.save(yeuCau);

        String moTa = "Đã nhận tiền cho mã " + phien.getMaThanhToan() + " nhưng không tạo được đơn hàng: " + lyDo;
        businessNotificationService.refundAccountNeeded(phien.getNguoiMua(), giaoDich.getSoTien(), moTa);
        businessNotificationService.transferPending(true, giaoDich.getSoTien(), moTa);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void createStorePayout(StoreOrder order) {
        if (transferRequestRepository.existsByLoaiAndDonHangCuaHang_MaDHCH(CHI_CUA_HANG, order.getMaDHCH())) {
            return;
        }
        BigDecimal soTien = nz(order.getDoanhThuShop());
        Store cuaHang = order.getCuaHang();
        if (soTien.signum() <= 0 || cuaHang == null) {
            return;
        }

        transferRequestRepository.save(TransferRequest.builder()
                .loai(CHI_CUA_HANG)
                .donHangCuaHang(order)
                .nguoiNhan(cuaHang.getChuShop())
                .soTien(soTien)
                .trangThai(TransferStatus.CHO_XU_LY.name())
                .build());

        businessNotificationService.transferPending(false, soTien,
                "Đơn #" + order.getMaDHCH() + " của \"" + cuaHang.getTenCuaHang() + "\" đã giao thành công.");
        if (!coTaiKhoan(cuaHang)) {
            businessNotificationService.payoutAccountNeeded(cuaHang, order.getMaDHCH());
        }
    }

    // ---------------------------------------------------------------
    // Khách hàng
    // ---------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<TransferRequestResponse> myRefunds(Integer maND) {
        return transferRequestRepository.findAllByLoaiAndNguoiNhan_MaNDOrderByMaYeuCauDesc(HOAN_TIEN_KHACH, maND).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TransferRequestResponse submitRefundAccount(Integer maND, Integer maYeuCau, BankAccountRequest request) {
        TransferRequest yeuCau = transferRequestRepository.findByIdForUpdate(maYeuCau)
                .filter(y -> HOAN_TIEN_KHACH.equals(y.getLoai()) && maND.equals(y.getNguoiNhan().getMaND()))
                .orElseThrow(() -> new BusinessException("Yêu cầu hoàn tiền không tồn tại"));

        if (TransferStatus.DA_CHUYEN.name().equals(yeuCau.getTrangThai())) {
            throw new BusinessException("Khoản này đã được hoàn, không thể đổi tài khoản nhận");
        }

        boolean lanDau = TransferStatus.CHO_THONG_TIN.name().equals(yeuCau.getTrangThai());

        yeuCau.setMaNganHang(request.maNganHang().trim());
        yeuCau.setSoTaiKhoan(request.soTaiKhoan().trim());
        yeuCau.setTenChuTaiKhoan(request.tenChuTaiKhoan().trim());
        yeuCau.setTrangThai(TransferStatus.CHO_XU_LY.name());
        transferRequestRepository.save(yeuCau);
        eventPublisher.publishEvent(new OperationsChangedEvent(OperationsChangedEvent.REFUNDS));

        if (lanDau) {
            businessNotificationService.transferPending(true, yeuCau.getSoTien(),
                    "Khách đã nhập tài khoản nhận cho khoản hoàn " + noiDungChuyenKhoan(yeuCau) + ".");
        }

        return toResponse(yeuCau);
    }

    // ---------------------------------------------------------------
    // Quản lý
    // ---------------------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<TransferRequestResponse> listTransfers(
            TransferType loai, TransferStatus trangThai, int page, int size
    ) {
        return listTransfers(loai, trangThai, null, page, size);
    }

    /** tuKhoa: mã đơn, tên / SĐT / email người nhận; trống thì không lọc. */
    @Transactional(readOnly = true)
    public PageResponse<TransferRequestResponse> listTransfers(
            TransferType loai, TransferStatus trangThai, String tuKhoa, int page, int size
    ) {
        String tk = tuKhoa == null ? "" : tuKhoa.trim().toLowerCase();
        return PageResponse.of(transferRequestRepository
                .timTheoLoaiVaTuKhoa(loai.name(), trangThai == null ? null : trangThai.name(),
                        tk, "%" + tk + "%", trang(page, size))
                .map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<BankTransactionResponse> listTransactions(int page, int size) {
        return PageResponse.of(bankTransactionRepository
                .findAllByOrderByMaGiaoDichDesc(trang(page, size))
                .map(BankTransactionResponse::from));
    }

    /** Giao dịch có lọc, kèm tổng số / tổng tiền khớp / tổng tiền lệch của toàn bộ kết quả lọc. */
    @Transactional(readOnly = true)
    public GiaoDichTrang listTransactions(
            String tuKhoa, ReconciliationResult ketQua, LocalDate tuNgay, LocalDate denNgay, int page, int size
    ) {
        String kq = ketQua == null ? "" : ketQua.name();
        LocalDateTime tu = (tuNgay == null ? LocalDate.of(2000, 1, 1) : tuNgay).atStartOfDay();
        LocalDateTime den = (denNgay == null ? LocalDate.of(2999, 1, 1) : denNgay).plusDays(1).atStartOfDay();
        String mau = "%" + (tuKhoa == null ? "" : tuKhoa.trim().toLowerCase()) + "%";

        var trangKetQua = bankTransactionRepository.loc(kq, tu, den, mau, trang(page, size)).map(BankTransactionResponse::from);
        var tomTat = bankTransactionRepository.tomTat(kq, tu, den, mau);
        return new GiaoDichTrang(trangKetQua.getContent(), trangKetQua.getNumber(), trangKetQua.getSize(),
                trangKetQua.getTotalElements(), trangKetQua.getTotalPages(),
                new TomTatGiaoDich(tomTat.getTongSo(), tomTat.getTongTienKhop(), tomTat.getTongTienLech()));
    }

    @Transactional
    public TransferRequestResponse markTransferred(Integer maTKXuLy, Integer maYeuCau) {
        TransferRequest yeuCau = transferRequestRepository.findByIdForUpdate(maYeuCau)
                .orElseThrow(() -> new BusinessException("Khoản chuyển tiền không tồn tại"));

        if (TransferStatus.DA_CHUYEN.name().equals(yeuCau.getTrangThai())) {
            throw new BusinessException("Khoản này đã được đánh dấu là đã chuyển");
        }

        boolean hoanTien = HOAN_TIEN_KHACH.equals(yeuCau.getLoai());
        Store cuaHang = yeuCau.getDonHangCuaHang() == null ? null : yeuCau.getDonHangCuaHang().getCuaHang();

        if (hoanTien && !TransferStatus.CHO_XU_LY.name().equals(yeuCau.getTrangThai())) {
            throw new BusinessException("Khách chưa nhập tài khoản nhận hoàn tiền");
        }
        if (!hoanTien && !coTaiKhoan(cuaHang)) {
            throw new BusinessException("Cửa hàng chưa khai tài khoản nhận tiền");
        }

        LocalDateTime now = LocalDateTime.now();
        yeuCau.setTrangThai(TransferStatus.DA_CHUYEN.name());
        yeuCau.setNguoiXuLy(accountRepository.getReferenceById(maTKXuLy));
        yeuCau.setNgayXuLy(now);
        transferRequestRepository.save(yeuCau);
        eventPublisher.publishEvent(new OperationsChangedEvent(hoanTien ? OperationsChangedEvent.REFUNDS : OperationsChangedEvent.PAYOUTS));

        // Khoản gắn với đơn hàng thì ghi thêm vào sổ thanh_toan, cùng chỗ với các khoản quyết toán khác.
        if (yeuCau.getDonHangCuaHang() != null) {
            StoreOrder order = yeuCau.getDonHangCuaHang();
            paymentRepository.save(Payment.builder()
                    .donHang(order.getDonHang())
                    .donHangCuaHang(order)
                    .congThanhToan(CONG_CHUYEN_KHOAN)
                    .loaiGiaoDich(hoanTien ? LOAI_GD_HOAN_TIEN : LOAI_GD_CHI_CUA_HANG)
                    .maGiaoDichCong(noiDungChuyenKhoan(yeuCau))
                    .soTien(yeuCau.getSoTien())
                    .trangThaiGiaoDich(TX_SUCCESS)
                    .thoiGianTT(now)
                    .build());
        }

        if (hoanTien) {
            businessNotificationService.refundCompleted(yeuCau.getNguoiNhan(), yeuCau.getSoTien());
        } else {
            businessNotificationService.storePaidOut(cuaHang, yeuCau.getDonHangCuaHang().getMaDHCH(), yeuCau.getSoTien());
        }

        return toResponse(yeuCau);
    }

    // ---------------------------------------------------------------

    private TransferRequestResponse toResponse(TransferRequest yeuCau) {
        StoreOrder order = yeuCau.getDonHangCuaHang();
        Store cuaHang = order == null ? null : order.getCuaHang();
        User nguoiNhan = yeuCau.getNguoiNhan();
        boolean chiCuaHang = CHI_CUA_HANG.equals(yeuCau.getLoai());

        // Khoản chi đọc tài khoản hiện tại của cửa hàng, nên cửa hàng khai sau vẫn được dùng ngay.
        String maNganHang = chiCuaHang && cuaHang != null ? cuaHang.getMaNganHang() : yeuCau.getMaNganHang();
        String soTaiKhoan = chiCuaHang && cuaHang != null ? cuaHang.getSoTaiKhoan() : yeuCau.getSoTaiKhoan();
        String tenChuTaiKhoan = chiCuaHang && cuaHang != null
                ? cuaHang.getTenChuTaiKhoan() : yeuCau.getTenChuTaiKhoan();
        String noiDung = noiDungChuyenKhoan(yeuCau);

        return new TransferRequestResponse(
                yeuCau.getMaYeuCau(),
                yeuCau.getLoai(),
                yeuCau.getTrangThai(),
                yeuCau.getSoTien(),
                noiDung,
                order == null ? null : order.getDonHang().getMaDH(),
                order == null ? null : order.getMaDHCH(),
                yeuCau.getLyDo(),
                nguoiNhan.getHoTen(),
                nguoiNhan.getEmail(),
                nguoiNhan.getSoDienThoai(),
                cuaHang == null ? null : cuaHang.getTenCuaHang(),
                maNganHang,
                soTaiKhoan,
                tenChuTaiKhoan,
                vietQrLinkBuilder.build(maNganHang, soTaiKhoan, tenChuTaiKhoan, yeuCau.getSoTien(), noiDung),
                yeuCau.getNgayTao(),
                yeuCau.getNgayXuLy(),
                order == null ? null : order.getHuyBoi(),
                order == null ? null : order.getLyDoHuy(),
                yeuCau.getMaDotChi()
        );
    }

    private static String noiDungChuyenKhoan(TransferRequest yeuCau) {
        return TransferType.valueOf(yeuCau.getLoai()).getTienTo() + yeuCau.getMaYeuCau();
    }

    private static void dienSanTaiKhoan(TransferRequest yeuCau, BankTransaction giaoDich) {
        yeuCau.setMaNganHang(giaoDich.getMaNganHangChuyen());
        yeuCau.setSoTaiKhoan(giaoDich.getSoTaiKhoanChuyen());
        yeuCau.setTenChuTaiKhoan(giaoDich.getTenNguoiChuyen());
    }


    private static boolean coTaiKhoan(Store cuaHang) {
        return cuaHang != null
                && cuaHang.getMaNganHang() != null && !cuaHang.getMaNganHang().isBlank()
                && cuaHang.getSoTaiKhoan() != null && !cuaHang.getSoTaiKhoan().isBlank();
    }

    private static PageRequest trang(int page, int size) {
        return PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100));
    }

    private static String cut(String value) {
        return value == null || value.length() <= 500 ? value : value.substring(0, 500);
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
