package com.oldbook.repository.payment;

import com.oldbook.entity.payment.VoucherUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface VoucherUsageRepository extends JpaRepository<VoucherUsage, Integer> {

    boolean existsByKhuyenMai_MaKM(Integer maKM);

    @Query("select distinct u.khuyenMai.maKM from VoucherUsage u")
    Set<Integer> findUsedPromotionIds();
}
