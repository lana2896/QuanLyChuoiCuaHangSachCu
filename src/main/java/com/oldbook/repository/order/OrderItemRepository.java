package com.oldbook.repository.order;

import com.oldbook.entity.order.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {

    List<OrderItem> findAllByDonHangCuaHang_MaDHCHOrderByMaCTDHAsc(Integer maDHCH);
}
