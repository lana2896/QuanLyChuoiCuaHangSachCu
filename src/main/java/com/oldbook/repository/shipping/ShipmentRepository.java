package com.oldbook.repository.shipping;

import com.oldbook.entity.shipping.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Integer> {

    Optional<Shipment> findByDonHangCuaHang_MaDHCH(Integer maDHCH);

    List<Shipment> findAllByDonHangCuaHang_MaDHCHIn(Collection<Integer> maDHCHs);

    boolean existsByMaVanDon(String maVanDon);
}
