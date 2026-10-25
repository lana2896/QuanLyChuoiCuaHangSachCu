package com.oldbook.repository.order;

import com.oldbook.dto.order.StoreOrderProductOption;
import com.oldbook.entity.order.OrderItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {

    List<OrderItem> findAllByDonHangCuaHang_MaDHCHOrderByMaCTDHAsc(Integer maDHCH);

    @Query("select new com.oldbook.dto.order.StoreOrderProductOption(ct.sach.maSach, ct.sach.tenSach) "
            + "from OrderItem ct where ct.donHangCuaHang.cuaHang.maCH = :maCH "
            + "group by ct.sach.maSach, ct.sach.tenSach order by ct.sach.tenSach")
    List<StoreOrderProductOption> findDistinctProductsByStore(@Param("maCH") Integer maCH);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ct from OrderItem ct where ct.maCTDH = :maCTDH")
    Optional<OrderItem> findByIdForUpdate(@Param("maCTDH") Integer maCTDH);
}
