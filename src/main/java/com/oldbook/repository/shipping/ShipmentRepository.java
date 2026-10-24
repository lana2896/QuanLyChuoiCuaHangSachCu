package com.oldbook.repository.shipping;

import com.oldbook.entity.shipping.Shipment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Integer> {

    Optional<Shipment> findByDonHangCuaHang_MaDHCH(Integer maDHCH);

    List<Shipment> findAllByDonHangCuaHang_MaDHCHIn(Collection<Integer> maDHCHs);

    boolean existsByMaVanDon(String maVanDon);



    // Toàn bộ vận đơn thuộc về MỘT đơn vị vận chuyển (mỗi trang chỉ thấy đơn của mình).
    @EntityGraph(attributePaths = {"donHangCuaHang", "donHangCuaHang.donHang", "donViVanChuyen"})
    List<Shipment> findAllByDonViVanChuyen_MaDVVCOrderByMaVanChuyenDesc(Integer maDVVC);

    @EntityGraph(attributePaths = {"donHangCuaHang", "donHangCuaHang.donHang", "donViVanChuyen"})
    Optional<Shipment> findByMaVanChuyenAndDonViVanChuyen_MaDVVC(Integer maVanChuyen, Integer maDVVC);

    // Chỉ lấy mã đơn con để khóa đơn trước rồi mới đọc vận đơn.
    // Có điều kiện theo đơn vị vận chuyển: hãng khác gọi API sẽ coi như vận đơn không tồn tại.
    @Query("select v.donHangCuaHang.maDHCH from Shipment v "
            + "where v.maVanChuyen = :id and v.donViVanChuyen.maDVVC = :maDVVC")
    Optional<Integer> findMaDHCHByIdAndDonVi(@Param("id") Integer id, @Param("maDVVC") Integer maDVVC);

    // ===== Tra cứu theo mã vận đơn / đồng bộ định kỳ =====

    Optional<Shipment> findByMaVanDon(String maVanDon);

    /** Mã đơn hàng con của một mã vận đơn (dùng để khóa đơn trước khi đọc lại vận đơn). */
    @Query("select v.donHangCuaHang.maDHCH from Shipment v where v.maVanDon = :maVanDon")
    Optional<Integer> findMaDHCHByMaVanDon(@Param("maVanDon") String maVanDon);

    /** Các vận đơn chưa kết thúc (phân trang theo khóa) để đồng bộ định kỳ. */
    @Query("""
            select v from Shipment v
            join fetch v.donViVanChuyen
            where v.trangThaiVanChuyen not in :finalStatuses
              and v.maVanChuyen > :afterId
            order by v.maVanChuyen asc
            """)
    List<Shipment> findActiveAfter(@Param("finalStatuses") Collection<String> finalStatuses,
                                    @Param("afterId") Integer afterId,
                                    Pageable pageable);

    @Query("""
            select v from Shipment v
            join fetch v.donViVanChuyen
            where v.donHangCuaHang.maDHCH = :maDHCH
            """)
    Optional<Shipment> findWithCarrierByMaDHCH(@Param("maDHCH") Integer maDHCH);

    @Query("select v.donViVanChuyen.maDVVC, count(v) from Shipment v group by v.donViVanChuyen.maDVVC")
    List<Object[]> demVanDonTheoDonVi();

    @Query("""
            select v.donViVanChuyen.maDVVC, count(v) from Shipment v
            where v.trangThaiVanChuyen not in :trangThaiKetThuc
            group by v.donViVanChuyen.maDVVC
            """)
    List<Object[]> demVanDonDangXuLyTheoDonVi(@Param("trangThaiKetThuc") Collection<String> trangThaiKetThuc);

    @Query("""
            select v.donViVanChuyen.maDVVC, count(v) from Shipment v
            where v.trangThaiVanChuyen = :trangThai
            group by v.donViVanChuyen.maDVVC
            """)
    List<Object[]> demVanDonTheoDonViVaTrangThai(@Param("trangThai") String trangThai);

    long countByDonViVanChuyen_MaDVVC(Integer maDVVC);

    long countByDonViVanChuyen_MaDVVCAndTrangThaiVanChuyenNotIn(Integer maDVVC, Collection<String> trangThaiKetThuc);

    long countByDonViVanChuyen_MaDVVCAndTrangThaiVanChuyen(Integer maDVVC, String trangThai);
}