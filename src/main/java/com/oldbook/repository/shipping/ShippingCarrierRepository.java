package com.oldbook.repository.shipping;

import com.oldbook.entity.shipping.ShippingCarrier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShippingCarrierRepository extends JpaRepository<ShippingCarrier, Integer> {

    List<ShippingCarrier> findAllByTrangThaiOrderByTenDonViAsc(String trangThai);

    List<ShippingCarrier> findAllByOrderByMaDVVCAsc();

    Optional<ShippingCarrier> findByMaDVVCAndTrangThai(Integer maDVVC, String trangThai);

    // Mã đơn vị vận chuyển (ma_code) là khóa định danh công khai: dùng trong URL trang /shipping/{maCode}
    // và các API /api/shipping/{maCode}/... Luôn lưu dạng IN HOA (xem ShippingCarrierResolver.normalize).
    Optional<ShippingCarrier> findByMaCode(String maCode);

    boolean existsByMaCode(String maCode);

}
