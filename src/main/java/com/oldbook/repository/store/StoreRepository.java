package com.oldbook.repository.store;

import com.oldbook.entity.catalog.Store;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Integer>, JpaSpecificationExecutor<Store> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Store c where c.maCH = :maCH")
    Optional<Store> findByIdForUpdate(@Param("maCH") Integer maCH);

    Optional<Store> findByChuShop_MaND(Integer maND);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Store c where c.chuShop.maND = :maND")
    Optional<Store> findByChuShopMaNDForUpdate(@Param("maND") Integer maND);
}
