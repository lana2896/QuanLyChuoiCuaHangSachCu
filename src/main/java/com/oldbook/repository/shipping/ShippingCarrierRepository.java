package com.oldbook.repository.shipping;

import com.oldbook.entity.shipping.ShippingCarrier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShippingCarrierRepository extends JpaRepository<ShippingCarrier, Integer> {

    List<ShippingCarrier> findAllByTrangThaiOrderByTenDonViAsc(String trangThai);

    Optional<ShippingCarrier> findByMaDVVCAndTrangThai(Integer maDVVC, String trangThai);
}
