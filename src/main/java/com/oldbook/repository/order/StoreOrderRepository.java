package com.oldbook.repository.order;

import com.oldbook.entity.order.StoreOrder;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Repository
public interface StoreOrderRepository extends JpaRepository<StoreOrder, Integer> {

    List<StoreOrder> findAllByDonHang_MaDHOrderByMaDHCHAsc(Integer maDH);

    List<StoreOrder> findAllByCuaHang_MaCHOrderByMaDHCHDesc(Integer maCH);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from StoreOrder d where d.maDHCH = :maDHCH")
    Optional<StoreOrder> findByIdForUpdate(@Param("maDHCH") Integer maDHCH);

    // ----- Truy vấn cho màn hình Quản lý (tổng quan / thống kê). Chuyển từ service xuống đây để đúng mô hình 3 lớp. -----

    @Query("""
            select coalesce(sum(d.commissionTien), 0) from StoreOrder d
            where d.trangThaiDonHang = :tt and d.ngayGiao >= :tu and d.ngayGiao < :den
            """)
    BigDecimal sumCommissionDeliveredBetween(@Param("tt") String trangThai,
                                             @Param("tu") LocalDateTime tu, @Param("den") LocalDateTime den);

    @Query("""
            select coalesce(sum(d.phiShipCH), 0) from StoreOrder d
            where d.trangThaiDonHang = :tt and d.ngayGiao >= :tu and d.ngayGiao < :den
            """)
    BigDecimal sumShippingFeeDeliveredBetween(@Param("tt") String trangThai,
                                              @Param("tu") LocalDateTime tu, @Param("den") LocalDateTime den);

    /** Kết quả giao theo hãng. Cột: 0 tên hãng, 1 số đơn kết thúc, 2 giao thành công, 3 hoàn trả. */
    @Query("""
            select v.tenDonVi, count(d),
                   sum(case when d.trangThaiDonHang = :giao then 1 else 0 end),
                   sum(case when d.trangThaiDonHang = :hoan then 1 else 0 end)
            from StoreOrder d join d.donViVanChuyen v
            where d.trangThaiDonHang in (:cuoi)
            group by v.maDVVC, v.tenDonVi
            order by count(d) desc
            """)
    List<Object[]> summarizeCarrierOutcomes(@Param("giao") String daGiao, @Param("hoan") String daHoanTra,
                                            @Param("cuoi") List<String> trangThaiCuoi);

    /**
     * Các đơn cửa hàng đặt trong kỳ. Cột: 0 maDHCH, 1 maCH, 2 tenCuaHang, 3 trạng thái, 4 huyBoi, 5 ngày đặt,
     * 6 ngày xác nhận, 7 ngày giao, 8 maDVVC (có thể null), 9 tên đơn vị vận chuyển.
     */
    @Query("""
            select d.maDHCH, c.maCH, c.tenCuaHang, d.trangThaiDonHang, d.huyBoi, dh.ngayTao,
                   d.ngayXacNhan, d.ngayGiao, v.maDVVC, v.tenDonVi
            from StoreOrder d join d.donHang dh join d.cuaHang c left join d.donViVanChuyen v
            where dh.ngayTao >= :tu and dh.ngayTao < :den
            """)
    List<Object[]> findPlacedBetweenForStats(@Param("tu") LocalDateTime tu, @Param("den") LocalDateTime den);

    /** Doanh thu đơn đã giao theo cửa hàng. Cột: 0 maCH, 1 tổng tiền hàng. */
    @Query("""
            select c.maCH, coalesce(sum(d.tienHangCH), 0) from StoreOrder d join d.cuaHang c
            where d.trangThaiDonHang = :tt and d.ngayGiao >= :tu and d.ngayGiao < :den group by c.maCH
            """)
    List<Object[]> sumRevenueByStoreDeliveredBetween(@Param("tt") String trangThai,
                                                     @Param("tu") LocalDateTime tu, @Param("den") LocalDateTime den);

    /** Đơn ở trạng thái tt đặt trước mốc han. Cột: 0 maDHCH, 1 maDH, 2 maCH, 3 tenCuaHang, 4 ngày đặt. */
    @Query("""
            select d.maDHCH, dh.maDH, c.maCH, c.tenCuaHang, dh.ngayTao
            from StoreOrder d join d.donHang dh join d.cuaHang c
            where d.trangThaiDonHang = :tt and dh.ngayTao < :han order by dh.ngayTao
            """)
    List<Object[]> findWaitingSince(@Param("tt") String trangThai, @Param("han") LocalDateTime han);

    /** Số đơn hủy theo bên hủy. Cột: 0 bên hủy (KHAC nếu trống), 1 số đơn. */
    @Query("""
            select coalesce(d.huyBoi, 'KHAC'), count(d) from StoreOrder d
            where d.trangThaiDonHang = :tt and d.ngayHuy >= :tu and d.ngayHuy < :den
            group by coalesce(d.huyBoi, 'KHAC')
            """)
    List<Object[]> countCancelledBySideBetween(@Param("tt") String trangThai,
                                               @Param("tu") LocalDateTime tu, @Param("den") LocalDateTime den);
}
