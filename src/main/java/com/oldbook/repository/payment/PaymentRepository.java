package com.oldbook.repository.payment;

import com.oldbook.entity.payment.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    boolean existsByDonHangCuaHang_MaDHCHAndLoaiGiaoDich(Integer maDHCH, String loaiGiaoDich);

    boolean existsByMaGiaoDichCongAndLoaiGiaoDich(String maGiaoDichCong, String loaiGiaoDich);

    // ----- Truy vấn cho màn hình Quản lý (tổng quan / thống kê). Chuyển từ service xuống đây để đúng mô hình 3 lớp. -----

    /** Tổng tiền các giao dịch thành công của một loại (ví dụ thu hộ COD) trong khoảng thời gian. */
    @Query("""
            select coalesce(sum(p.soTien), 0) from Payment p
            where p.loaiGiaoDich = :loai and p.trangThaiGiaoDich = 'THANH_CONG'
              and p.thoiGianTT >= :tu and p.thoiGianTT < :den
            """)
    BigDecimal sumSuccessfulBetween(@Param("loai") String loaiGiaoDich,
                                    @Param("tu") LocalDateTime tu, @Param("den") LocalDateTime den);
}
