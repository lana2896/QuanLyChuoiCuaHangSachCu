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
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Số liệu màn hình Tổng quan của Quản lý. Chỉ đọc.
 * Hoa hồng tính cùng quy tắc với báo cáo doanh thu của Admin (AdminReportService): đơn DA_GIAO, mốc là ngày giao.
 */
@Service
public class ManagementOverviewService {

    private static final int SO_VIEC_TOI_DA = 5;
    private static final int SO_NGAY_GIAO_DICH_LECH = 30;

    private static final String HOAN = TransferType.HOAN_TIEN_KHACH.name();
    private static final String CHI = TransferType.CHI_CUA_HANG.name();
    private static final String CHO_THONG_TIN = TransferStatus.CHO_THONG_TIN.name();
    private static final String CHO_XU_LY = TransferStatus.CHO_XU_LY.name();
    private static final String DA_CHUYEN = TransferStatus.DA_CHUYEN.name();

    @PersistenceContext
    private EntityManager em;

    @Transactional(readOnly = true)
    public HuyHieu huyHieu() {
        long kiemDuyet = dem("select count(c) from Store c where c.trangThaiDuyet = :tt", "tt", cho())
                + dem("select count(s) from Book s where s.trangThaiDuyet = :tt", "tt", cho());
        long thanhToan = demYeuCau(HOAN, CHO_XU_LY) + demYeuCau(CHI, CHO_XU_LY);
        return new HuyHieu(kiemDuyet, thanhToan);
    }

    @Transactional(readOnly = true)
    public TongQuan tongQuan() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dauThang = now.toLocalDate().withDayOfMonth(1).atStartOfDay();
        LocalDateTime dauThangSau = dauThang.plusMonths(1);

        long choDuyetCuaHang = dem("select count(c) from Store c where c.trangThaiDuyet = :tt", "tt", cho());
        long choDuyetSach = dem("select count(s) from Book s where s.trangThaiDuyet = :tt", "tt", cho());
        long hoanCho = demYeuCau(HOAN, CHO_XU_LY);
        long hoanChoKhachKhai = demYeuCau(HOAN, CHO_THONG_TIN);

        Object[] chi = (Object[]) em.createQuery("""
                        select count(y), coalesce(sum(y.soTien), 0), count(distinct d.cuaHang.maCH)
                        from TransferRequest y join y.donHangCuaHang d
                        where y.loai = :loai and y.trangThai = :tt
                        """)
                .setParameter("loai", CHI).setParameter("tt", CHO_XU_LY)
                .getSingleResult();

        BigDecimal hoaHongThang = tong("""
                select coalesce(sum(d.commissionTien), 0) from StoreOrder d
                where d.trangThaiDonHang = :tt and d.ngayGiao >= :tu and d.ngayGiao < :den
                """, OrderStatus.DA_GIAO.name(), dauThang, dauThangSau);

        long lech = ((Number) em.createQuery("""
                        select count(g) from BankTransaction g
                        where g.ketQua in (:kq) and g.ngayNhan >= :tu
                        """)
                .setParameter("kq", List.of(ReconciliationResult.KHONG_KHOP.name(), ReconciliationResult.CAN_HOAN_TIEN.name()))
                .setParameter("tu", now.minusDays(SO_NGAY_GIAO_DICH_LECH))
                .getSingleResult()).longValue();

        BigDecimal khachDaTra = (BigDecimal) em.createQuery("""
                        select coalesce(sum(g.soTien), 0) from BankTransaction g
                        where g.ketQua in (:kq) and g.ngayNhan >= :tu and g.ngayNhan < :den
                        """)
                .setParameter("kq", List.of(ReconciliationResult.KHOP.name(), ReconciliationResult.CAN_HOAN_TIEN.name()))
                .setParameter("tu", dauThang).setParameter("den", dauThangSau)
                .getSingleResult();

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

        for (Object[] r : hang("""
                select c.maCH, c.tenCuaHang, c.ngayTao from Store c
                where c.trangThaiDuyet = :tt order by c.ngayTao desc
                """, "tt", cho())) {
            viec.add(new ViecCho("CUA_HANG_CHO_DUYET", "Cửa hàng chờ duyệt",
                    "\"" + r[1] + "\" đăng ký gian hàng", "/store-moderation", "Kiểm duyệt ngay", (LocalDateTime) r[2]));
        }
        for (Object[] r : hang("""
                select s.maSach, s.tuaSach.tenSach, s.ngayTao from Book s
                where s.trangThaiDuyet = :tt order by s.ngayTao desc
                """, "tt", cho())) {
            viec.add(new ViecCho("SACH_CHO_DUYET", "Sách chờ duyệt",
                    "\"" + r[1] + "\"", "/store-moderation?tab=books", "Xem sách", (LocalDateTime) r[2]));
        }
        for (Object[] r : hang("""
                select y.maYeuCau, y.soTien, y.ngayTao from TransferRequest y
                where y.loai = :loai and y.trangThai = :tt order by y.ngayTao desc
                """, "loai", HOAN, "tt", CHO_THONG_TIN)) {
            viec.add(new ViecCho("HOAN_TIEN_CHO_KHACH", "Hoàn tiền chờ khách khai tài khoản",
                    "Khoản HT" + r[0] + " · " + dong((BigDecimal) r[1]), "/management/payments?tab=refunds", "Nhắc khai STK", (LocalDateTime) r[2]));
        }
        for (Object[] r : hang("""
                select y.maYeuCau, y.soTien, y.ngayTao from TransferRequest y
                where y.loai = :loai and y.trangThai = :tt order by y.ngayTao desc
                """, "loai", HOAN, "tt", CHO_XU_LY)) {
            viec.add(new ViecCho("HOAN_TIEN_CHO_CHUYEN", "Hoàn tiền chờ chuyển khoản",
                    "Khoản HT" + r[0] + " · " + dong((BigDecimal) r[1]), "/management/payments?tab=refunds", "Hoàn tiền", (LocalDateTime) r[2]));
        }
        for (Object[] r : hang("""
                select c.maCH, c.tenCuaHang, y.soTien, y.ngayTao
                from TransferRequest y join y.donHangCuaHang d join d.cuaHang c
                where y.loai = :loai and y.trangThai = :tt order by y.ngayTao desc
                """, "loai", CHI, "tt", CHO_XU_LY)) {
            viec.add(new ViecCho("CHI_CUA_HANG", "Cửa hàng chờ nhận tiền",
                    "\"" + r[1] + "\" · " + dong((BigDecimal) r[2]), "/management/payments?tab=payouts", "Tạo đợt chi", (LocalDateTime) r[3]));
        }
        for (Object[] r : hang("""
                select g.maGiaoDich, g.soTien, g.ngayNhan from BankTransaction g
                where g.ketQua in (:kq) order by g.ngayNhan desc
                """, "kq", List.of(ReconciliationResult.KHONG_KHOP.name(), ReconciliationResult.CAN_HOAN_TIEN.name()))) {
            viec.add(new ViecCho("GIAO_DICH_LECH", "Giao dịch ngân hàng bị lệch",
                    "Giao dịch #" + r[0] + " · " + dong((BigDecimal) r[1]), "/management/payments?tab=transactions", "Xem giao dịch", (LocalDateTime) r[2]));
        }

        viec.sort(Comparator.comparing(ViecCho::thoiGian, Comparator.nullsLast(Comparator.reverseOrder())));
        return viec.size() > SO_VIEC_TOI_DA ? new ArrayList<>(viec.subList(0, SO_VIEC_TOI_DA)) : viec;
    }

    private List<DoiTacVanChuyen> vanChuyen() {
        List<Object[]> rows = em.createQuery("""
                        select v.tenDonVi, count(d),
                               sum(case when d.trangThaiDonHang = :giao then 1 else 0 end),
                               sum(case when d.trangThaiDonHang = :hoan then 1 else 0 end)
                        from StoreOrder d join d.donViVanChuyen v
                        where d.trangThaiDonHang in (:cuoi)
                        group by v.maDVVC, v.tenDonVi
                        order by count(d) desc
                        """, Object[].class)
                .setParameter("giao", OrderStatus.DA_GIAO.name())
                .setParameter("hoan", OrderStatus.DA_HOAN_TRA.name())
                .setParameter("cuoi", List.of(OrderStatus.DA_GIAO.name(), OrderStatus.DA_HOAN_TRA.name()))
                .getResultList();
        return rows.stream()
                .map(r -> new DoiTacVanChuyen((String) r[0], ((Number) r[1]).longValue(),
                        ((Number) r[2]).longValue(), ((Number) r[3]).longValue()))
                .toList();
    }

    private BigDecimal daChuyenTrongThang(String loai, LocalDateTime tu, LocalDateTime den) {
        return (BigDecimal) em.createQuery("""
                        select coalesce(sum(y.soTien), 0) from TransferRequest y
                        where y.loai = :loai and y.trangThai = :tt and y.ngayXuLy >= :tu and y.ngayXuLy < :den
                        """)
                .setParameter("loai", loai).setParameter("tt", DA_CHUYEN)
                .setParameter("tu", tu).setParameter("den", den)
                .getSingleResult();
    }

    private BigDecimal tong(String jpql, String tt, LocalDateTime tu, LocalDateTime den) {
        return (BigDecimal) em.createQuery(jpql)
                .setParameter("tt", tt).setParameter("tu", tu).setParameter("den", den)
                .getSingleResult();
    }

    private long demYeuCau(String loai, String trangThai) {
        return ((Number) em.createQuery("select count(y) from TransferRequest y where y.loai = :loai and y.trangThai = :tt")
                .setParameter("loai", loai).setParameter("tt", trangThai)
                .getSingleResult()).longValue();
    }

    private long dem(String jpql, String ten, Object giaTri) {
        return ((Number) em.createQuery(jpql).setParameter(ten, giaTri).getSingleResult()).longValue();
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> hang(String jpql, Object... thamSo) {
        var query = em.createQuery(jpql).setMaxResults(SO_VIEC_TOI_DA);
        for (int i = 0; i < thamSo.length; i += 2) {
            query.setParameter((String) thamSo[i], thamSo[i + 1]);
        }
        return query.getResultList();
    }

    private static String cho() {
        return ApprovalStatus.CHO_DUYET.name();
    }

    private static String dong(BigDecimal soTien) {
        return java.text.NumberFormat.getIntegerInstance(java.util.Locale.forLanguageTag("vi-VN")).format(soTien) + " đ";
    }
}
