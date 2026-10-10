package com.oldbook.repository.shipping;

import com.oldbook.entity.shipping.ShipmentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ShipmentHistoryRepository extends JpaRepository<ShipmentHistory, Integer> {

    List<ShipmentHistory> findAllByVanChuyen_MaVanChuyenInOrderByThoiGianAscMaLichSuAsc(
            Collection<Integer> maVanChuyens);
}
