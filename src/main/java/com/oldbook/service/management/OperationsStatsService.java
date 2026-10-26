package com.oldbook.service.management;

import com.oldbook.constant.catalog.ApprovalStatus;
import com.oldbook.constant.order.OrderStatus;
import com.oldbook.constant.payment.ReconciliationResult;
import com.oldbook.constant.payment.TransferType;
import com.oldbook.constant.payment.TransferStatus;
import com.oldbook.dto.management.ManagementDto.HoaHongDanhMuc;
import com.oldbook.dto.management.ManagementDto.KetQuaNhac;
import com.oldbook.dto.management.StatsDto.*;
import com.oldbook.entity.catalog.Store;
import com.oldbook.exception.common.BusinessException;
import com.oldbook.repository.notification.NotificationRepository;
import com.oldbook.repository.store.StoreRepository;
import com.oldbook.service.notification.BusinessNotificationService;
import com.oldbook.service.order.OrderSettlementService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Thống kê vận hành, chỉ đọc. Quản lý xem để hành động (ai chậm, ai lỗi, việc gì đang tồn);
 * Admin dùng cùng service này để xem tổng và xuất CSV, nên các con số luôn khớp nhau.
 *
 * Quy tắc tiền (khớp AdminReportService): chiết khấu là commissionTien của đơn DA_GIAO, mốc là ngày giao.
 * Các tỉ lệ theo cửa hàng và theo hãng vận chuyển tính trên các đơn ĐẶT trong kỳ.
 */
@Service
@RequiredArgsConstructor
public class OperationsStatsService {

    private static final int SO_NGAY_MAC_DINH = 30;
    private static final int SO_NGAY_TOI_DA = 366;
    private static final long GIO_QUA_HAN = 48;

    private static final String DA_GIAO = OrderStatus.DA_GIAO.name();
    private static final String DA_HOAN_TRA = OrderStatus.DA_HOAN_TRA.name();
    private static final String DA_HUY = OrderStatus.DA_HUY.name();
    private static final String CHO_XU_LY = OrderStatus.CHO_XU_LY.name();
    private static final String HOAN = TransferType.HOAN_TIEN_KHACH.name();
    private static final String CHI = TransferType.CHI_CUA_HANG.name();
    private static final String DA_CHUYEN = TransferStatus.DA_CHUYEN.name();
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final CommissionManagementService commissionManagementService;
    private final StoreRepository storeRepository;
    private final NotificationRepository notificationRepository;
    private final BusinessNotificationService businessNotificationService;

    @PersistenceContext
    private EntityManager em;

    @Transactional(readOnly = true)
    public ThongKe thongKe(LocalDate tuNgay, LocalDate denNgay) {
        LocalDate den = denNgay != null ? denNgay : LocalDate.now();
        LocalDate tu = tuNgay != null ? tuNgay : den.minusDays(SO_NGAY_MAC_DINH - 1L);
        kiemTraKy(tu, den);
        LocalDateTime tuLuc = tu.atStartOfDay();
        LocalDateTime denLuc = den.plusDays(1).atStartOfDay();

        List<Object[]> donHang = donDatTrongKy(tuLuc, denLuc);
        return new ThongKe(tu, den,
                dongTien(tuLuc, denLuc),
                cuaHang(donHang, tuLuc, denLuc),
                donQuaHan(),
                vanChuyen(donHang),
                huyHoan(tuLuc, denLuc),
                danhMuc(tuLuc, denLuc),
                kiemDuyet());
    }

    /** Dòng tiền toàn chuỗi, dùng riêng cho Admin. */
    @Transactional(readOnly = true)
    public DongTienSan dongTien(LocalDate tuNgay, LocalDate denNgay) {
        LocalDate den = denNgay != null ? denNgay : LocalDate.now();
        LocalDate tu = tuNgay != null ? tuNgay : den.minusDays(SO_NGAY_MAC_DINH - 1L);
        kiemTraKy(tu, den);
        return dongTien(tu.atStartOfDay(), den.plusDays(1).atStartOfDay());
    }

    /** Doanh thu và chiết khấu theo danh mục, dùng riêng cho Admin. */
    @Transactional(readOnly = true)
    public List<DanhMucThongKe> danhMuc(LocalDate tuNgay, LocalDate denNgay) {
        LocalDate den = denNgay != null ? denNgay : LocalDate.now();
        LocalDate tu = tuNgay != null ? tuNgay : den.minusDays(SO_NGAY_MAC_DINH - 1L);
        kiemTraKy(tu, den);
        return danhMuc(tu.atStartOfDay(), den.plusDays(1).atStartOfDay());
    }

    /** Nhắc cửa hàng xác nhận các đơn đang chờ quá 48 giờ. Mỗi cửa hàng nhắc tối đa một lần mỗi giờ. */
    @Transactional
    public KetQuaNhac nhacXacNhan(Integer maCH) {
        Store ch = storeRepository.findById(maCH).orElseThrow(() -> new BusinessException("Cửa hàng không tồn tại"));
        long soDon = donQuaHan().stream().filter(d -> d.maCH().equals(maCH)).count();
        if (soDon == 0) {
            throw new BusinessException("Cửa hàng không có đơn nào chờ xác nhận quá " + GIO_QUA_HAN + " giờ");
        }
        notificationRepository
                .findFirstByLoaiAndTaiKhoanNhan_NguoiDung_MaNDOrderByNgayTaoDesc(
                        BusinessNotificationService.LOAI_NHAC_XAC_NHAN_DON, ch.getChuShop().getMaND())
                .filter(t -> t.getNgayTao().isAfter(LocalDateTime.now().minusHours(1)))
                .ifPresent(t -> {
                    throw new BusinessException("Đã nhắc lúc " + String.format("%02d:%02d",
                            t.getNgayTao().getHour(), t.getNgayTao().getMinute()) + ", chỉ nhắc lại được sau 1 giờ");
                });
        businessNotificationService.remindConfirmOrders(ch, soDon);
        return new KetQuaNhac(LocalDateTime.now());
    }

    // ---------------------------------------------------------------

    private DongTienSan dongTien(LocalDateTime tu, LocalDateTime den) {
        BigDecimal vao = tong("""
                select coalesce(sum(g.soTien), 0) from BankTransaction g
                where g.ketQua in (:kq) and g.ngayNhan >= :tu and g.ngayNhan < :den
                """, "kq", List.of(ReconciliationResult.KHOP.name(), ReconciliationResult.CAN_HOAN_TIEN.name()), tu, den);
        BigDecimal thuHoCod = tong("""
                select coalesce(sum(p.soTien), 0) from Payment p
                where p.loaiGiaoDich = :loai and p.trangThaiGiaoDich = 'THANH_CONG'
                  and p.thoiGianTT >= :tu and p.thoiGianTT < :den
                """, "loai", OrderSettlementService.TYPE_COD_COLLECTION, tu, den);
        vao = vao.add(thuHoCod);
        BigDecimal hoan = daChuyen(HOAN, tu, den);
        BigDecimal chi = daChuyen(CHI, tu, den);
        BigDecimal hoaHong = tong("""
                select coalesce(sum(d.commissionTien), 0) from StoreOrder d
                where d.trangThaiDonHang = :tt and d.ngayGiao >= :tu and d.ngayGiao < :den
                """, "tt", DA_GIAO, tu, den);
        BigDecimal ship = tong("""
                select coalesce(sum(d.phiShipCH), 0) from StoreOrder d
                where d.trangThaiDonHang = :tt and d.ngayGiao >= :tu and d.ngayGiao < :den
                """, "tt", DA_GIAO, tu, den);
        long lech = ((Number) em.createQuery("""
                        select count(g) from BankTransaction g
                        where g.ketQua in (:kq) and g.ngayNhan >= :tu and g.ngayNhan < :den
                        """)
                .setParameter("kq", List.of(ReconciliationResult.KHONG_KHOP.name(), ReconciliationResult.CAN_HOAN_TIEN.name()))
                .setParameter("tu", tu).setParameter("den", den).getSingleResult()).longValue();
        return new DongTienSan(vao, hoan, chi, hoaHong, ship,
                vao.subtract(hoan).subtract(chi).subtract(hoaHong).subtract(ship), lech);
    }

    /**
     * Các đơn cửa hàng đặt trong kỳ. Cột: 0 maDHCH, 1 maCH, 2 tenCuaHang, 3 trạng thái, 4 huyBoi, 5 ngày đặt,
     * 6 ngày xác nhận, 7 ngày giao, 8 maDVVC (có thể null), 9 tên đơn vị vận chuyển.
     */
    @SuppressWarnings("unchecked")
    private List<Object[]> donDatTrongKy(LocalDateTime tu, LocalDateTime den) {
        return em.createQuery("""
                        select d.maDHCH, c.maCH, c.tenCuaHang, d.trangThaiDonHang, d.huyBoi, dh.ngayTao,
                               d.ngayXacNhan, d.ngayGiao, v.maDVVC, v.tenDonVi
                        from StoreOrder d join d.donHang dh join d.cuaHang c left join d.donViVanChuyen v
                        where dh.ngayTao >= :tu and dh.ngayTao < :den
                        """)
                .setParameter("tu", tu).setParameter("den", den).getResultList();
    }

    private List<CuaHangThongKe> cuaHang(List<Object[]> don, LocalDateTime tu, LocalDateTime den) {
        Map<Integer, String> ten = new LinkedHashMap<>();
        Map<Integer, long[]> dem = new LinkedHashMap<>();            // 0 tổng, 1 hoàn tất, 2 hủy bởi chi nhánh, 3 số đơn đã xác nhận
        Map<Integer, Double> gioXacNhan = new LinkedHashMap<>();    // tổng giờ
        for (Object[] r : don) {
            Integer maCH = (Integer) r[1];
            ten.put(maCH, (String) r[2]);
            long[] c = dem.computeIfAbsent(maCH, k -> new long[4]);
            c[0]++;
            if (DA_GIAO.equals(r[3])) { c[1]++; }
            if (DA_HUY.equals(r[3]) && "CHU_CUA_HANG".equals(r[4])) { c[2]++; }
            if (r[6] != null && r[5] != null) {
                c[3]++;
                gioXacNhan.merge(maCH, gio((LocalDateTime) r[5], (LocalDateTime) r[6]), Double::sum);
            }
        }

        Map<Integer, BigDecimal> revenue = new LinkedHashMap<>();
        for (Object[] r : em.createQuery("""
                        select c.maCH, coalesce(sum(d.tienHangCH), 0) from StoreOrder d join d.cuaHang c
                        where d.trangThaiDonHang = :tt and d.ngayGiao >= :tu and d.ngayGiao < :den group by c.maCH
                        """, Object[].class)
                .setParameter("tt", DA_GIAO).setParameter("tu", tu).setParameter("den", den).getResultList()) {
            revenue.put((Integer) r[0], (BigDecimal) r[1]);
            ten.putIfAbsent((Integer) r[0], null);
        }

        Map<Integer, double[]> sao = new LinkedHashMap<>();         // 0 tổng sao, 1 số đánh giá
        for (Object[] r : em.createQuery("""
                        select s.cuaHang.maCH, avg(g.soSao), count(g) from Review g
                        join g.chiTietDonHang ct join ct.sach s group by s.cuaHang.maCH
                        """, Object[].class).getResultList()) {
            sao.put((Integer) r[0], new double[]{((Number) r[1]).doubleValue(), ((Number) r[2]).doubleValue()});
        }

        List<CuaHangThongKe> ds = new ArrayList<>();
        for (Integer maCH : ten.keySet()) {
            long[] c = dem.getOrDefault(maCH, new long[4]);
            String tenCh = ten.get(maCH) != null ? ten.get(maCH)
                    : storeRepository.findById(maCH).map(Store::getTenCuaHang).orElse("Cửa hàng #" + maCH);
            double[] s = sao.get(maCH);
            ds.add(new CuaHangThongKe(maCH, tenCh, revenue.getOrDefault(maCH, BigDecimal.ZERO), c[1], c[0], c[2],
                    phanTram(c[2], c[0]),
                    c[3] == 0 ? null : BigDecimal.valueOf(gioXacNhan.getOrDefault(maCH, 0.0) / c[3]).setScale(1, RoundingMode.HALF_UP),
                    s == null ? null : BigDecimal.valueOf(s[0]).setScale(1, RoundingMode.HALF_UP),
                    s == null ? 0 : (long) s[1]));
        }
        ds.sort(Comparator.comparing(CuaHangThongKe::revenue).reversed());
        return ds;
    }

    /** Đơn đang CHO_XU_LY mà khách đặt đã quá 48 giờ. */
    private List<DonQuaHan> donQuaHan() {
        LocalDateTime now = LocalDateTime.now();
        List<Object[]> rows = em.createQuery("""
                        select d.maDHCH, dh.maDH, c.maCH, c.tenCuaHang, dh.ngayTao
                        from StoreOrder d join d.donHang dh join d.cuaHang c
                        where d.trangThaiDonHang = :tt and dh.ngayTao < :han order by dh.ngayTao
                        """, Object[].class)
                .setParameter("tt", CHO_XU_LY).setParameter("han", now.minusHours(GIO_QUA_HAN)).getResultList();
        return rows.stream().map(r -> new DonQuaHan((Integer) r[0], (Integer) r[1], (Integer) r[2], (String) r[3],
                (LocalDateTime) r[4], Duration.between((LocalDateTime) r[4], now).toHours())).toList();
    }

    private List<VanChuyenThongKe> vanChuyen(List<Object[]> don) {
        Map<Integer, String> ten = new LinkedHashMap<>();
        Map<Integer, long[]> dem = new LinkedHashMap<>();           // 0 kết thúc, 1 giao thành công, 2 hoàn trả, 3 số đơn có thời gian giao
        Map<Integer, Double> gioGiao = new LinkedHashMap<>();
        for (Object[] r : don) {
            if (r[8] == null) { continue; }
            Integer ma = (Integer) r[8];
            boolean giao = DA_GIAO.equals(r[3]);
            boolean hoan = DA_HOAN_TRA.equals(r[3]);
            if (!giao && !hoan) { continue; }
            ten.put(ma, (String) r[9]);
            long[] c = dem.computeIfAbsent(ma, k -> new long[4]);
            c[0]++;
            if (giao) { c[1]++; }
            if (hoan) { c[2]++; }
            if (giao && r[6] != null && r[7] != null) {
                c[3]++;
                gioGiao.merge(ma, gio((LocalDateTime) r[6], (LocalDateTime) r[7]), Double::sum);
            }
        }
        List<VanChuyenThongKe> ds = new ArrayList<>();
        for (Integer ma : ten.keySet()) {
            long[] c = dem.get(ma);
            ds.add(new VanChuyenThongKe(ten.get(ma), c[0], c[1], c[2], phanTram(c[1], c[0]), phanTram(c[2], c[0]),
                    c[3] == 0 ? null : BigDecimal.valueOf(gioGiao.getOrDefault(ma, 0.0) / c[3]).setScale(1, RoundingMode.HALF_UP)));
        }
        ds.sort(Comparator.comparingLong(VanChuyenThongKe::soDon).reversed());
        return ds;
    }

    private HuyHoan huyHoan(LocalDateTime tu, LocalDateTime den) {
        List<HuyTheoBen> theoBen = em.createQuery("""
                        select coalesce(d.huyBoi, 'KHAC'), count(d) from StoreOrder d
                        where d.trangThaiDonHang = :tt and d.ngayHuy >= :tu and d.ngayHuy < :den
                        group by coalesce(d.huyBoi, 'KHAC')
                        """, Object[].class)
                .setParameter("tt", DA_HUY).setParameter("tu", tu).setParameter("den", den).getResultList().stream()
                .map(r -> new HuyTheoBen((String) r[0], ((Number) r[1]).longValue())).toList();

        // Tiền hoàn theo tuần (tuần bắt đầu thứ Hai), gom ở Java để không phụ thuộc hàm ngày của từng DBMS.
        Map<LocalDate, BigDecimal[]> tuan = new TreeMap<>();
        List<Object[]> hoan = em.createQuery("""
                        select y.soTien, y.ngayXuLy, y.ngayTao from TransferRequest y
                        where y.loai = :loai and y.trangThai = :tt and y.ngayXuLy >= :tu and y.ngayXuLy < :den
                        """, Object[].class)
                .setParameter("loai", HOAN).setParameter("tt", DA_CHUYEN).setParameter("tu", tu).setParameter("den", den)
                .getResultList();
        double tongGio = 0;
        for (Object[] r : hoan) {
            LocalDate dauTuan = ((LocalDateTime) r[1]).toLocalDate().with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
            BigDecimal[] a = tuan.computeIfAbsent(dauTuan, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            a[0] = a[0].add((BigDecimal) r[0]);
            a[1] = a[1].add(BigDecimal.ONE);
            tongGio += gio((LocalDateTime) r[2], (LocalDateTime) r[1]);
        }
        List<HoanTheoTuan> theoTuan = tuan.entrySet().stream()
                .map(e -> new HoanTheoTuan(e.getKey(), e.getValue()[0], e.getValue()[1].longValue())).toList();
        return new HuyHoan(theoBen, theoTuan,
                hoan.isEmpty() ? null : BigDecimal.valueOf(tongGio / hoan.size()).setScale(1, RoundingMode.HALF_UP));
    }

    private List<DanhMucThongKe> danhMuc(LocalDateTime tu, LocalDateTime den) {
        Map<Integer, HoaHongDanhMuc> hienHanh = new LinkedHashMap<>();
        commissionManagementService.danhSach().forEach(h -> hienHanh.put(h.maDM(), h));

        List<Object[]> rows = em.createQuery("""
                        select dm.maDM, dm.tenDanhMuc, coalesce(sum(ct.thanhTien), 0), coalesce(sum(ct.tienChietKhau), 0)
                        from OrderItem ct join ct.donHangCuaHang d join ct.sach s join s.tuaSach ts join ts.danhMuc dm
                        where d.trangThaiDonHang = :tt and d.ngayGiao >= :tu and d.ngayGiao < :den
                        group by dm.maDM, dm.tenDanhMuc
                        """, Object[].class)
                .setParameter("tt", DA_GIAO).setParameter("tu", tu).setParameter("den", den).getResultList();
        Map<Integer, Object[]> theoMa = new LinkedHashMap<>();
        rows.forEach(r -> theoMa.put((Integer) r[0], r));

        List<DanhMucThongKe> ds = new ArrayList<>();
        for (HoaHongDanhMuc h : hienHanh.values()) {
            Object[] r = theoMa.get(h.maDM());
            ds.add(new DanhMucThongKe(h.maDM(), h.tenDanhMuc(),
                    r == null ? BigDecimal.ZERO : (BigDecimal) r[2], r == null ? BigDecimal.ZERO : (BigDecimal) r[3], h.tyLe()));
        }
        ds.sort(Comparator.comparing(DanhMucThongKe::revenue).reversed());
        return ds;
    }

    private KiemDuyetThongKe kiemDuyet() {
        String cho = ApprovalStatus.CHO_DUYET.name();
        String tuChoi = ApprovalStatus.TU_CHOI.name();
        String daDuyet = ApprovalStatus.DA_DUYET.name();
        long cuaHangCho = dem("select count(c) from Store c where c.trangThaiDuyet = :tt", cho);
        long sachCho = dem("select count(s) from Book s where s.trangThaiDuyet = :tt", cho);
        long tuChoiSo = dem("select count(c) from Store c where c.trangThaiDuyet = :tt", tuChoi)
                + dem("select count(s) from Book s where s.trangThaiDuyet = :tt", tuChoi);
        long daXuLy = tuChoiSo + dem("select count(c) from Store c where c.trangThaiDuyet = :tt", daDuyet)
                + dem("select count(s) from Book s where s.trangThaiDuyet = :tt", daDuyet);
        return new KiemDuyetThongKe(cuaHangCho, sachCho, daXuLy == 0 ? null : phanTram(tuChoiSo, daXuLy));
    }

    // ---------------------------------------------------------------

    private void kiemTraKy(LocalDate tu, LocalDate den) {
        if (tu.isAfter(den)) {
            throw new BusinessException("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.");
        }
        if (ChronoUnit.DAYS.between(tu, den) + 1 > SO_NGAY_TOI_DA) {
            throw new BusinessException("Kỳ thống kê tối đa " + SO_NGAY_TOI_DA + " ngày.");
        }
    }

    private BigDecimal daChuyen(String loai, LocalDateTime tu, LocalDateTime den) {
        return (BigDecimal) em.createQuery("""
                        select coalesce(sum(y.soTien), 0) from TransferRequest y
                        where y.loai = :loai and y.trangThai = :tt and y.ngayXuLy >= :tu and y.ngayXuLy < :den
                        """)
                .setParameter("loai", loai).setParameter("tt", DA_CHUYEN)
                .setParameter("tu", tu).setParameter("den", den).getSingleResult();
    }

    private BigDecimal tong(String jpql, String ten, Object giaTri, LocalDateTime tu, LocalDateTime den) {
        return (BigDecimal) em.createQuery(jpql).setParameter(ten, giaTri)
                .setParameter("tu", tu).setParameter("den", den).getSingleResult();
    }

    private long dem(String jpql, String trangThai) {
        return ((Number) em.createQuery(jpql).setParameter("tt", trangThai).getSingleResult()).longValue();
    }

    private static double gio(LocalDateTime tu, LocalDateTime den) {
        return Duration.between(tu, den).toMinutes() / 60.0;
    }

    private static BigDecimal phanTram(long tu, long mau) {
        return mau == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(tu).multiply(HUNDRED).divide(BigDecimal.valueOf(mau), 1, RoundingMode.HALF_UP);
    }
}
