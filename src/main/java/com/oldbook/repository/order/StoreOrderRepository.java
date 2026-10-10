package com.oldbook.repository.order;

import com.oldbook.entity.order.StoreOrder;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreOrderRepository extends JpaRepository<StoreOrder, Integer> {

    List<StoreOrder> findAllByCuaHang_MaCHOrderByMaDHCHDesc(Integer maCH);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from StoreOrder d where d.maDHCH = :maDHCH")
    Optional<StoreOrder> findByIdForUpdate(@Param("maDHCH") Integer maDHCH);

}
