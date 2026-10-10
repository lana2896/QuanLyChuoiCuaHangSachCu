package com.oldbook.repository.cart;

import com.oldbook.entity.cart.ChiTietGioHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChiTietGioHangRepository extends JpaRepository<ChiTietGioHang, Integer> {

    @Query("""
            select ct from ChiTietGioHang ct
            join fetch ct.sach s
            join fetch s.cuaHang
            where ct.gioHang.maGioHang = :maGioHang
            order by ct.maCTGioHang desc
            """)
    List<ChiTietGioHang> findAllByGioHang(@Param("maGioHang") Integer maGioHang);

    @Query("""
            select ct from ChiTietGioHang ct
            where ct.gioHang.maGioHang = :maGioHang
              and ct.sach.maSach = :maSach
            """)
    Optional<ChiTietGioHang> findByGioHangAndSach(
            @Param("maGioHang") Integer maGioHang,
            @Param("maSach") Integer maSach
    );

    @Query("""
            select ct from ChiTietGioHang ct
            join fetch ct.gioHang g
            where ct.maCTGioHang = :maCTGioHang
              and g.nguoiDung.maND = :maND
            """)
    Optional<ChiTietGioHang> findByIdAndOwner(
            @Param("maCTGioHang") Integer maCTGioHang,
            @Param("maND") Integer maND
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ChiTietGioHang ct where ct.gioHang.maGioHang = :maGioHang")
    void deleteAllByGioHang(@Param("maGioHang") Integer maGioHang);
}
