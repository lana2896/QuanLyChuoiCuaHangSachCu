package com.oldbook.repository.cart;

import com.oldbook.entity.cart.GioHang;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GioHangRepository extends JpaRepository<GioHang, Integer> {

    @Query("select g from GioHang g where g.nguoiDung.maND = :maND")
    Optional<GioHang> findByMaND(@Param("maND") Integer maND);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from GioHang g where g.maGioHang = :maGioHang")
    Optional<GioHang> findByIdForUpdate(@Param("maGioHang") Integer maGioHang);
}
