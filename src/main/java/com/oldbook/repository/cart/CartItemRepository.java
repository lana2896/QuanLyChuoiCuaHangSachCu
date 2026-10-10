package com.oldbook.repository.cart;

import com.oldbook.entity.cart.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Integer> {

    @Query("""
            select ct from CartItem ct
            join fetch ct.sach s
            join fetch s.cuaHang
            where ct.gioHang.maGioHang = :maGioHang
            order by ct.maCTGioHang desc
            """)
    List<CartItem> findAllByGioHang(@Param("maGioHang") Integer maGioHang);

    @Query("""
            select ct from CartItem ct
            where ct.gioHang.maGioHang = :maGioHang
              and ct.sach.maSach = :maSach
            """)
    Optional<CartItem> findByGioHangAndSach(
            @Param("maGioHang") Integer maGioHang,
            @Param("maSach") Integer maSach
    );

    @Query("""
            select ct from CartItem ct
            join fetch ct.gioHang g
            where ct.maCTGioHang = :maCTGioHang
              and g.nguoiDung.maND = :maND
            """)
    Optional<CartItem> findByIdAndOwner(
            @Param("maCTGioHang") Integer maCTGioHang,
            @Param("maND") Integer maND
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from CartItem ct where ct.gioHang.maGioHang = :maGioHang")
    void deleteAllByGioHang(@Param("maGioHang") Integer maGioHang);
}
