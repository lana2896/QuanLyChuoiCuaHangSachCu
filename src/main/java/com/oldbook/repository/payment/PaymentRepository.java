package com.oldbook.repository.payment;

import com.oldbook.entity.payment.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    boolean existsByDonHangCuaHang_MaDHCHAndLoaiGiaoDich(Integer maDHCH, String loaiGiaoDich);

    boolean existsByMaGiaoDichCongAndLoaiGiaoDich(String maGiaoDichCong, String loaiGiaoDich);
}
