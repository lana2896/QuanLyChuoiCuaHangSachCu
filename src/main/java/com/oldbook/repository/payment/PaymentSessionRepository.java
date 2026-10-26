package com.oldbook.repository.payment;

import com.oldbook.entity.payment.PaymentSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentSessionRepository extends JpaRepository<PaymentSession, Integer> {

    // Mọi thay đổi trạng thái phiên đều khóa dòng trước, để hết hạn, hủy và tiền về không chạy chồng lên nhau.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PaymentSession p where p.maPhien = :maPhien")
    Optional<PaymentSession> findByIdForUpdate(@Param("maPhien") Integer maPhien);

    Optional<PaymentSession> findByMaThanhToan(String maThanhToan);

    boolean existsByMaThanhToan(String maThanhToan);

    Optional<PaymentSession> findFirstByDonHang_MaDH(Integer maDH);

    @Query("select p.maPhien from PaymentSession p where p.nguoiMua.maND = :maND and p.trangThai = :trangThai")
    List<Integer> findIdsByNguoiMuaAndTrangThai(@Param("maND") Integer maND, @Param("trangThai") String trangThai);

    @Query("select p.maPhien from PaymentSession p where p.trangThai = :trangThai and p.hetHanLuc < :luc")
    List<Integer> findIdsHetHan(@Param("trangThai") String trangThai, @Param("luc") LocalDateTime luc);
}
