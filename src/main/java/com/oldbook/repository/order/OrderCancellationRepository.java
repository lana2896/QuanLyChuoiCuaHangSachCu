package com.oldbook.repository.order;

import com.oldbook.entity.order.OrderCancellation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderCancellationRepository extends JpaRepository<OrderCancellation, Integer> {

    List<OrderCancellation> findAllByDonHangCuaHang_MaDHCHIn(Collection<Integer> maDHCHs);

    boolean existsByDonHangCuaHang_MaDHCH(Integer maDHCH);

    boolean existsByDonHangCuaHang_MaDHCHAndTrangThaiYeuCau(Integer maDHCH, String trangThaiYeuCau);

    Optional<OrderCancellation> findFirstByDonHangCuaHang_MaDHCHAndTrangThaiYeuCau(Integer maDHCH, String trangThaiYeuCau);
}
