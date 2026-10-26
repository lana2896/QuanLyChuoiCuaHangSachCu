package com.oldbook.repository.payment;

import com.oldbook.entity.payment.Promotion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Integer> {

    Optional<Promotion> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    List<Promotion> findAllByLoaiVoucherOrderByMaKMDesc(String loaiVoucher);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Promotion p where p.maKM = :maKM")
    Optional<Promotion> findByIdForUpdate(@Param("maKM") Integer maKM);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Promotion p where upper(p.code) = upper(:code)")
    Optional<Promotion> findByCodeForUpdate(@Param("code") String code);

    List<Promotion> findAllByCuaHang_MaCHAndLoaiVoucherOrderByMaKMDesc(Integer maCH, String loaiVoucher);

    @Query("""
            select p from Promotion p
            where p.loaiVoucher = :loaiVoucher
              and p.cuaHang.maCH in :maCHs
              and p.trangThai = :trangThai
              and p.ngayBatDau <= :now
              and p.ngayKetThuc >= :now
              and coalesce(p.daSuDung, 0) < p.soLuong
            order by p.ngayKetThuc asc, p.maKM desc
            """)
    List<Promotion> findAvailableByStores(
            @Param("loaiVoucher") String loaiVoucher,
            @Param("maCHs") List<Integer> maCHs,
            @Param("trangThai") String trangThai,
            @Param("now") LocalDateTime now
    );

    @Query("""
            select p from Promotion p
            where p.loaiVoucher = :loaiVoucher
              and p.trangThai = :trangThai
              and p.ngayBatDau <= :now
              and p.ngayKetThuc >= :now
              and coalesce(p.daSuDung, 0) < p.soLuong
            order by p.ngayKetThuc asc, p.maKM desc
            """)
    List<Promotion> findAvailable(
            @Param("loaiVoucher") String loaiVoucher,
            @Param("trangThai") String trangThai,
            @Param("now") LocalDateTime now
    );
}
