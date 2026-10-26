package com.oldbook.repository.payment;

import com.oldbook.entity.payment.TransferRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TransferRequestRepository extends JpaRepository<TransferRequest, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select y from TransferRequest y where y.maYeuCau = :maYeuCau")
    Optional<TransferRequest> findByIdForUpdate(@Param("maYeuCau") Integer maYeuCau);

    // Mỗi đơn hàng con chỉ có một khoản hoàn và một khoản chi.
    boolean existsByLoaiAndDonHangCuaHang_MaDHCH(String loai, Integer maDHCH);

    boolean existsByGiaoDichNganHang_MaGiaoDich(Integer maGiaoDich);

    List<TransferRequest> findAllByLoaiAndNguoiNhan_MaNDOrderByMaYeuCauDesc(String loai, Integer maND);

    @Query("""
            select y from TransferRequest y
            where y.loai = :loai and (:trangThai is null or y.trangThai = :trangThai)
            order by y.maYeuCau desc
            """)
    Page<TransferRequest> timTheoLoai(
            @Param("loai") String loai, @Param("trangThai") String trangThai, Pageable pageable);
}
