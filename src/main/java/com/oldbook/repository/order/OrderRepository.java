package com.oldbook.repository.order;

import com.oldbook.entity.order.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {

    @Query("""
            select distinct dh from Order dh
            left join fetch dh.diaChiGiao
            where dh.nguoiMua.maND = :maND
            order by dh.ngayTao desc, dh.maDH desc
            """)
    List<Order> findAllByNguoiMua(@Param("maND") Integer maND);

    @Modifying
    @Query("update Order dh set dh.diaChiGiao = null where dh.diaChiGiao.maDiaChi = :maDiaChi")
    void detachAddress(@Param("maDiaChi") Integer maDiaChi);
}
