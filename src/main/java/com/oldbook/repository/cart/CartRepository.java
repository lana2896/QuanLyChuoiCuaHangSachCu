package com.oldbook.repository.cart;

import com.oldbook.entity.cart.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Integer> {

    @Query("select g from Cart g where g.nguoiDung.maND = :maND")
    Optional<Cart> findByMaND(@Param("maND") Integer maND);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from Cart g where g.maGioHang = :maGioHang")
    Optional<Cart> findByIdForUpdate(@Param("maGioHang") Integer maGioHang);
}
