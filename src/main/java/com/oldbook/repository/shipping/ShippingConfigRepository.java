package com.oldbook.repository.shipping;

import com.oldbook.entity.shipping.ShippingConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShippingConfigRepository extends JpaRepository<ShippingConfig, Integer> {
}
