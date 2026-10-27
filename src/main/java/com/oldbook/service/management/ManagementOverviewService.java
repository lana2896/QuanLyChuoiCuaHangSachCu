package com.oldbook.service.management;

import com.oldbook.constant.catalog.ApprovalStatus;
import com.oldbook.constant.order.OrderStatus;
import com.oldbook.constant.payment.ReconciliationResult;
import com.oldbook.constant.payment.TransferType;
import com.oldbook.constant.payment.TransferStatus;
import com.oldbook.dto.management.ManagementDto.DoiTacVanChuyen;
import com.oldbook.dto.management.ManagementDto.DongTien;
import com.oldbook.dto.management.ManagementDto.HuyHieu;
import com.oldbook.dto.management.ManagementDto.TongQuan;
import com.oldbook.dto.management.ManagementDto.ViecCho;
import com.oldbook.entity.catalog.Book;
import com.oldbook.entity.catalog.Store;
import com.oldbook.entity.payment.BankTransaction;
import com.oldbook.entity.payment.TransferRequest;
import com.oldbook.repository.catalog.BookRepository;
import com.oldbook.repository.order.StoreOrderRepository;
import com.oldbook.repository.payment.BankTransactionRepository;
import com.oldbook.repository.payment.PaymentRepository;
import com.oldbook.repository.payment.TransferRequestRepository;
import com.oldbook.repository.store.StoreRepository;
import com.oldbook.service.order.OrderSettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Số liệu màn hình Tổng quan của Quản lý. Chỉ đọc.
 * Chiết khấu tính cùng quy tắc với báo cáo doanh thu của Admin (AdminReportService): đơn DA_GIAO, mốc là ngày giao.
 * Mọi truy vấn nằm ở lớp repository; service chỉ ghép số liệu.
 */
@Service
@RequiredArgsConstructor
public class ManagementOverviewService {

    private static final int SO_VIEC_TOI_DA = 5;
    private static final int SO_NGAY_GIAO_DICH_LECH = 30;

    private static final String HOAN = TransferType.HOAN_TIEN_KHACH.name();
    private static final String CHI = TransferType.CHI_CUA_HANG.name();
    private static final String CHO_THONG_TIN = TransferStatus.CHO_THONG_TIN.name();
    private static final String CHO_XU_LY = TransferStatus.CHO_XU_LY.name();
    private static final String DA_CHUYEN = TransferStatus.DA_CHUYEN.name();

    private static final List<String> GIAO_DICH_LECH =
            List.of(ReconciliationResult.KHONG_KHOP.name(), ReconciliationResult.CAN_HOAN_TIEN.name());
    private static final List<String> GIAO_DICH_CO_TIEN_VAO =
            List.of(ReconciliationResult.KHOP.name(), ReconciliationResult.CAN_HOAN_TIEN.name());

    private final StoreRepository storeRepository;
    private final BookRepository bookRepository;
    private final TransferRequestRepository transferRequestRepository;
    private final BankTransactionRepository bankTransactionRepository;
    private final PaymentRepository paymentRepository;
    private final StoreOrderRepository storeOrderRepository;

    @Transactional(readOnly = true)
    public HuyHieu huyHieu() {
        long kiemDuyet = storeRepository.countByTrangThaiDuyet(cho()) + bookRepository.countByTrangThaiDuyet(cho());
        long thanhToan = demYeuCau(HOAN, CHO_XU_LY) + demYeuCau(CHI, CHO_XU_LY);
        return new HuyHieu(kiemDuyet, thanhToan);
    }

    @Transactional(readOnly = true)
    public TongQuan tongQuan() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dauThang = now.toLocalDate().withDayOfMonth(1).atStartOfDay();
        LocalDateTime dauThangSau = dauThang.plusMonths(1);

        long choDuyetCuaHang = storeRepository.countByTrangThaiDuyet(cho());
        long choDuyetSach = bookRepository.countByTrangThaiDuyet(cho());
        long hoanCho = demYeuCau(HOAN, CHO_XU_LY);
        long hoanChoKhachKhai = demYeuCau(HOAN, CHO_THONG_TIN);

        Object[] chi = transferRequestRepository.summarizeStorePayouts(CHI, CHO_XU_LY).get(0);

        BigDecimal hoaHongThang = storeOrderRepository.sumCommissionDeliveredBetween(
                OrderStatus.DA_GIAO.name(), dauThang, dauThangSau);

        // Không có giao dịch "trong tương lai", nên mốc cuối là ngày mai để lấy hết từ 30 ngày trước đến nay.
        long lech = bankTransactionRepository.countByResultsBetween(
                GIAO_DICH_LECH, now.minusDays(SO_NGAY_GIAO_DICH_LECH), now.plusDays(1));

        BigDecimal khachDaTra = bankTransactionRepository.sumByResultsBetween(GIAO_DICH_CO_TIEN_VAO, dauThang, dauThangSau)
                .add(paymentRepository.sumSuccessfulBetween(OrderSettlementService.TYPE_COD_COLLECTION, dauThang, dauThangSau));

        DongTien dongTien = new DongTien(
                khachDaTra,
                daChuyenTrongThang(HOAN, dauThang, dauThangSau),
                daChuyenTrongThang(CHI, dauThang, dauThangSau),
                hoaHongThang);

        return new TongQuan(
                choDuyetCuaHang, choDuyetSach, hoanCho, hoanChoKhachKhai,
                ((Number) chi[2]).longValue(), ((Number) chi[0]).longValue(), (BigDecimal) chi[1],
                hoaHongThang, lech,
                new HuyHieu(choDuyetCuaHang + choDuyetSach, hoanCho + ((Number) chi[0]).longValue()),
                hangDoi(),
                dongTien,
                vanChuyen(),
                now);
    }

    // ---------------------------------------------------------------

    /** 5 việc mới nhất cần Quản lý xử lý, gộp từ các hàng đợi. */
    private List<ViecCho> hangDoi() {
        List<ViecCho> viec = new ArrayList<>();
        Pageable dau = PageRequest.of(0, SO_VIEC_TOI_DA);

        for (Store c : storeRepository.findByTrangThaiDuyetOrderByNgayTaoDesc(cho(), dau)) {
            viec.add(new ViecCho("CUA_HANG_CHO_DUYET", "Cửa hàng chờ duyệt",
                    "\"" + c.getTenCuaHang() + "\" đăng ký mở chi nhánh", "/store-moderation", "Kiểm duyệt ngay", c.getNgayTao()));
        }
        for (Book s : bookRepository.findByTrangThaiDuyetOrderByNgayTaoDesc(cho(), dau)) {
            viec.add(new ViecCho("SACH_CHO_DUYET", "Sách chờ duyệt",
                    "\"" + s.getTuaSach().getTenSach() + "\"", "/store-moderation?tab=books", "Xem sách", s.getNgayTao()));
        }
        for (TransferRequest y : transferRequestRepository.findByLoaiAndTrangThaiOrderByNgayTaoDesc(HOAN, CHO_THONG_TIN, dau)) {
            viec.add(new ViecCho("HOAN_TIEN_CHO_KHACH", "Hoàn tiền chờ khách khai tài khoản",
                    "Khoản HT" + y.getMaYeuCau() + " · " + dong(y.getSoTien()), "/management/payments?tab=refunds", "Nhắc khai STK", y.getNgayTao()));
        }
        for (TransferRequest y : transferRequestRepository.findByLoaiAndTrangThaiOrderByNgayTaoDesc(HOAN, CHO_XU_LY, dau)) {
            viec.add(new ViecCho("HOAN_TIEN_CHO_CHUYEN", "Hoàn tiền chờ chuyển khoản",
                    "Khoản HT" + y.getMaYeuCau() + " · " + dong(y.getSoTien()), "/management/payments?tab=refunds", "Hoàn tiền", y.getNgayTao()));
        }
        for (TransferRequest y : transferRequestRepository.findByLoaiAndTrangThaiOrderByNgayTaoDesc(CHI, CHO_XU_LY, dau)) {
            Store c = y.getDonHangCuaHang().getCuaHang();
            viec.add(new ViecCho("CHI_CUA_HANG", "Cửa hàng chờ nhận tiền",
                    "\"" + c.getTenCuaHang() + "\" · " + dong(y.getSoTien()), "/management/payments?tab=payouts", "Tạo đợt chi", y.getNgayTao()));
        }
        for (BankTransaction g : bankTransactionRepository.findByKetQuaInOrderByNgayNhanDesc(GIAO_DICH_LECH, dau)) {
            viec.add(new ViecCho("GIAO_DICH_LECH", "Giao dịch ngân hàng bị lệch",
                    "Giao dịch #" + g.getMaGiaoDich() + " · " + dong(g.getSoTien()), "/management/payments?tab=transactions", "Xem giao dịch", g.getNgayNhan()));
        }

        viec.sort(Comparator.comparing(ViecCho::thoiGian, Comparator.nullsLast(Comparator.reverseOrder())));
        return viec.size() > SO_VIEC_TOI_DA ? new ArrayList<>(viec.subList(0, SO_VIEC_TOI_DA)) : viec;
    }

    private List<DoiTacVanChuyen> vanChuyen() {
        List<Object[]> rows = storeOrderRepository.summarizeCarrierOutcomes(
                OrderStatus.DA_GIAO.name(), OrderStatus.DA_HOAN_TRA.name(),
                List.of(OrderStatus.DA_GIAO.name(), OrderStatus.DA_HOAN_TRA.name()));
        return rows.stream()
                .map(r -> new DoiTacVanChuyen((String) r[0], ((Number) r[1]).longValue(),
                        ((Number) r[2]).longValue(), ((Number) r[3]).longValue()))
                .toList();
    }

    private BigDecimal daChuyenTrongThang(String loai, LocalDateTime tu, LocalDateTime den) {
        return transferRequestRepository.sumProcessedBetween(loai, DA_CHUYEN, tu, den);
    }

    private long demYeuCau(String loai, String trangThai) {
        return transferRequestRepository.countByLoaiAndTrangThai(loai, trangThai);
    }

    private static String cho() {
        return ApprovalStatus.CHO_DUYET.name();
    }

    private static String dong(BigDecimal soTien) {
        return java.text.NumberFormat.getIntegerInstance(java.util.Locale.forLanguageTag("vi-VN")).format(soTien) + " đ";
    }
}
