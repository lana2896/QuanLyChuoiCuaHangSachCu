package com.oldbook.repository.inventory;

import com.oldbook.entity.inventory.StockTransfer;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StockTransferRepository extends JpaRepository<StockTransfer, Integer> {

    /** Phiếu gửi đi và phiếu gửi đến của một chi nhánh, mới nhất trước. */
    @EntityGraph(attributePaths = {"sachNguon", "sachNguon.tuaSach", "cuaHangNguon", "cuaHangDich"})
    @Query("""
            select p from StockTransfer p
            where p.cuaHangNguon.maCH = :maCH or p.cuaHangDich.maCH = :maCH
            order by p.ngayTao desc, p.maPhieu desc
            """)
    List<StockTransfer> findAllOfStore(@Param("maCH") Integer maCH);

    @EntityGraph(attributePaths = {"sachNguon", "sachNguon.tuaSach", "cuaHangNguon", "cuaHangDich"})
    List<StockTransfer> findAllByOrderByNgayTaoDescMaPhieuDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"sachNguon", "sachNguon.tuaSach", "cuaHangNguon", "cuaHangDich"})
    List<StockTransfer> findAllByTrangThaiOrderByNgayTaoDescMaPhieuDesc(String trangThai, Pageable pageable);

    long countByTrangThai(String trangThai);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from StockTransfer p where p.maPhieu = :maPhieu")
    Optional<StockTransfer> findByIdForUpdate(@Param("maPhieu") Integer maPhieu);
}
