package com.oldbook.service.management;

import com.oldbook.constant.catalog.SaleStatus;
import com.oldbook.dto.management.ManagementDto.HoaHongDanhMuc;
import com.oldbook.dto.management.ManagementDto.HoaHongLichSu;
import com.oldbook.entity.catalog.Category;
import com.oldbook.entity.payment.CommissionConfig;
import com.oldbook.entity.system.SystemLog;
import com.oldbook.exception.common.BusinessException;
import com.oldbook.repository.catalog.BookRepository;
import com.oldbook.repository.catalog.CategoryRepository;
import com.oldbook.repository.identity.AccountRepository;
import com.oldbook.repository.payment.CommissionConfigRepository;
import com.oldbook.repository.system.SystemLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Quản lý tỷ lệ chiết khấu theo danh mục (bảng cau_hinh_chiet_khau).
 *
 * Tỷ lệ được lấy lúc đơn GIAO THÀNH CÔNG (OrderSettlementService), không phải lúc đặt, nên tỷ lệ mới áp cho
 * các đơn giao thành công sau thời điểm đổi. Mỗi lần đổi đóng bản ghi đang hiệu lực rồi thêm bản ghi mới, giữ nguyên lịch sử.
 */
@Service
@RequiredArgsConstructor
public class CommissionManagementService {

    public static final String HOAT_DONG = "HOAT_DONG";
    public static final String NGUNG = "NGUNG_AP_DUNG";

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final CategoryRepository categoryRepository;
    private final CommissionConfigRepository cauHinhRepository;
    private final AccountRepository accountRepository;
    private final SystemLogRepository nhatKyRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final BookRepository bookRepository;

    @Value("${marketplace.commission.default-rate:5.00}")
    private BigDecimal tyLeMacDinh;

    @Transactional(readOnly = true)
    public List<HoaHongDanhMuc> danhSach() {
        LocalDateTime now = LocalDateTime.now();
        Map<Integer, Long> soCuaHang = soCuaHangTheoDanhMuc();

        return categoryRepository.findAll().stream()
                .sorted((a, b) -> a.getTenDanhMuc().compareToIgnoreCase(b.getTenDanhMuc()))
                .map(dm -> {
                    CommissionConfig cfg = hieuLuc(dm.getMaDM(), now);
                    long ch = soCuaHang.getOrDefault(dm.getMaDM(), 0L);
                    if (cfg == null) {
                        return new HoaHongDanhMuc(dm.getMaDM(), dm.getTenDanhMuc(), tyLeMacDinh, true, ch,
                                null, null, null, null);
                    }
                    return new HoaHongDanhMuc(dm.getMaDM(), dm.getTenDanhMuc(), cfg.getTyLeChietKhau(), false, ch,
                            cfg.getNgayBatDau(), cfg.getNgayBatDau(), tenNguoi(cfg.getMaTKCapNhat()), cfg.getGhiChu());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HoaHongLichSu> lichSu(Integer maDM) {
        danhMuc(maDM);
        return cauHinhRepository.findByDanhMuc_MaDMOrderByNgayBatDauDescMaCauHinhChietKhauDesc(maDM).stream()
                .map(c -> new HoaHongLichSu(c.getMaCauHinhChietKhau(), c.getTyLeChietKhau(), c.getTrangThai(),
                        c.getNgayBatDau(), c.getNgayKetThuc(), tenNguoi(c.getMaTKCapNhat()), c.getGhiChu()))
                .toList();
    }

    @Transactional
    public HoaHongDanhMuc doiTyLe(Integer maTK, Integer maDM, BigDecimal tyLe, String ghiChu) {
        Category dm = danhMuc(maDM);
        if (tyLe == null || tyLe.signum() < 0 || tyLe.compareTo(HUNDRED) > 0) {
            throw new BusinessException("Tỷ lệ chiết khấu phải từ 0 đến 100%");
        }
        BigDecimal moi = tyLe.setScale(2, RoundingMode.HALF_UP);
        String note = ghiChu == null || ghiChu.isBlank() ? null : ghiChu.trim();
        if (note != null && note.length() > 500) {
            throw new BusinessException("Ghi chú tối đa 500 ký tự");
        }

        LocalDateTime now = LocalDateTime.now();
        CommissionConfig cu = hieuLuc(maDM, now);
        BigDecimal tyLeCu = cu == null ? tyLeMacDinh : cu.getTyLeChietKhau();
        if (cu != null && tyLeCu.compareTo(moi) == 0) {
            throw new BusinessException("Tỷ lệ mới trùng với tỷ lệ đang áp dụng");
        }

        // Đóng mọi bản ghi đang hiệu lực (thường chỉ một) rồi thêm bản ghi mới.
        for (CommissionConfig c : cauHinhRepository.findEffective(maDM, HOAT_DONG, now)) {
            c.setNgayKetThuc(now);
            c.setTrangThai(NGUNG);
            cauHinhRepository.save(c);
        }
        cauHinhRepository.save(CommissionConfig.builder()
                .danhMuc(dm).tyLeChietKhau(moi).trangThai(HOAT_DONG).ngayBatDau(now)
                .maTKCapNhat(maTK).ghiChu(note).build());

        nhatKyRepository.save(SystemLog.builder()
                .taiKhoan(accountRepository.getReferenceById(maTK))
                .hanhDong("DOI_HOA_HONG").loaiDoiTuong("DANH_MUC").maDoiTuong(maDM)
                .moTa("Danh mục \"" + dm.getTenDanhMuc() + "\": " + tyLeCu + "% -> " + moi + "%"
                        + (note == null ? "" : ". Ghi chú: " + note))
                .build());

        eventPublisher.publishEvent(new OperationsChangedEvent(OperationsChangedEvent.COMMISSIONS));

        return danhSach().stream().filter(h -> h.maDM().equals(maDM)).findFirst().orElseThrow();
    }

    // ---------------------------------------------------------------

    private Category danhMuc(Integer maDM) {
        return categoryRepository.findById(maDM).orElseThrow(() -> new BusinessException("Danh mục không tồn tại"));
    }

    private CommissionConfig hieuLuc(Integer maDM, LocalDateTime now) {
        return cauHinhRepository.findEffective(maDM, HOAT_DONG, now).stream()
                .filter(c -> c.getTyLeChietKhau() != null).findFirst().orElse(null);
    }

    private String tenNguoi(Integer maTK) {
        if (maTK == null) {
            return null;
        }
        return accountRepository.findById(maTK).map(t -> t.getNguoiDung().getHoTen()).orElse(null);
    }

    /** Số cửa hàng có sách đang bán trong từng danh mục. */
    private Map<Integer, Long> soCuaHangTheoDanhMuc() {
        List<Object[]> rows = bookRepository.countStoresSellingByCategory(SaleStatus.DANG_BAN.name());
        return rows.stream().collect(Collectors.toMap(r -> (Integer) r[0], r -> ((Number) r[1]).longValue(), (a, b) -> a));
    }
}
