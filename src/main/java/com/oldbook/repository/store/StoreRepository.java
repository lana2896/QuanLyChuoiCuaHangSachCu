package com.oldbook.repository.store;

import com.oldbook.entity.catalog.Store;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

public interface StoreRepository extends JpaRepository<Store, Integer>, JpaSpecificationExecutor<Store> {
    Optional<Store> findByChuShop_MaND(Integer maND);

    /** Các chi nhánh theo trạng thái duyệt, xếp theo số chi nhánh (dùng cho Hệ thống chi nhánh). */
    @EntityGraph(attributePaths = {"tinh", "xa"})
    List<Store> findAllByTrangThaiDuyetOrderBySoChiNhanhAsc(String trangThaiDuyet);

    @Query("select coalesce(max(c.soChiNhanh), 0) from Store c")
    int findMaxSoChiNhanh();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Store c where c.maCH = :maCH")
    Optional<Store> findByIdForUpdate(@Param("maCH") Integer maCH);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Store c where c.chuShop.maND = :maND")
    Optional<Store> findByChuShopMaNDForUpdate(@Param("maND") Integer maND);

    // ----- Truy vấn cho màn hình Quản lý (tổng quan / thống kê). Chuyển từ service xuống đây để đúng mô hình 3 lớp. -----

    long countByTrangThaiDuyet(String trangThaiDuyet);

    List<Store> findByTrangThaiDuyetOrderByNgayTaoDesc(String trangThaiDuyet, Pageable pageable);
}
