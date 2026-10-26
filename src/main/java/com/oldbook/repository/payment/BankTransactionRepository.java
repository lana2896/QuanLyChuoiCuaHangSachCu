package com.oldbook.repository.payment;

import com.oldbook.entity.payment.BankTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, Integer> {

    Optional<BankTransaction> findByMaGiaoDichCong(String maGiaoDichCong);

    Page<BankTransaction> findAllByOrderByMaGiaoDichDesc(Pageable pageable);

    /** Điều kiện lọc dùng chung cho danh sách và 3 số tóm tắt. Mọi tham số đều có giá trị (rỗng = không lọc). */
    String LOC = """
            from BankTransaction g left join g.phien p
            where (:ketQua = '' or g.ketQua = :ketQua)
              and g.ngayNhan >= :tu and g.ngayNhan < :den
              and (:mau = '%%'
                   or lower(g.maGiaoDichCong) like :mau or lower(coalesce(g.noiDung, '')) like :mau
                   or lower(coalesce(g.tenNguoiChuyen, '')) like :mau or coalesce(g.soTaiKhoanChuyen, '') like :mau
                   or lower(coalesce(p.maThanhToan, '')) like :mau)
            """;

    @Query("select g " + LOC + " order by g.maGiaoDich desc")
    Page<BankTransaction> loc(@Param("ketQua") String ketQua, @Param("tu") LocalDateTime tu,
                               @Param("den") LocalDateTime den, @Param("mau") String mau, Pageable pageable);

    @Query("""
            select count(g) as tongSo,
                   coalesce(sum(case when g.ketQua = 'KHOP' then g.soTien else 0 end), 0) as tongTienKhop,
                   coalesce(sum(case when g.ketQua in ('KHONG_KHOP', 'CAN_HOAN_TIEN') then g.soTien else 0 end), 0) as tongTienLech
            """ + LOC)
    TomTat tomTat(@Param("ketQua") String ketQua, @Param("tu") LocalDateTime tu,
                  @Param("den") LocalDateTime den, @Param("mau") String mau);

    interface TomTat {
        long getTongSo();

        BigDecimal getTongTienKhop();

        BigDecimal getTongTienLech();
    }

    Optional<BankTransaction> findFirstByPhien_MaPhienAndKetQuaOrderByMaGiaoDichDesc(Integer maPhien, String ketQua);
}
