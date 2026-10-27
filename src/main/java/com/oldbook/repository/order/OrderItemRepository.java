package com.oldbook.repository.order;

import com.oldbook.dto.order.StoreOrderProductOption;
import com.oldbook.entity.order.OrderItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {

    List<OrderItem> findAllByDonHangCuaHang_MaDHCHOrderByMaCTDHAsc(Integer maDHCH);

    @Query("select new com.oldbook.dto.order.StoreOrderProductOption(ct.sach.maSach, ct.sach.tuaSach.tenSach) "
            + "from OrderItem ct where ct.donHangCuaHang.cuaHang.maCH = :maCH "
            + "group by ct.sach.maSach, ct.sach.tuaSach.tenSach order by ct.sach.tuaSach.tenSach")
    List<StoreOrderProductOption> findDistinctProductsByStore(@Param("maCH") Integer maCH);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ct from OrderItem ct where ct.maCTDH = :maCTDH")
    Optional<OrderItem> findByIdForUpdate(@Param("maCTDH") Integer maCTDH);

    // ----- Truy vấn cho màn hình Quản lý (tổng quan / thống kê). Chuyển từ service xuống đây để đúng mô hình 3 lớp. -----

    /** Doanh thu và chiết khấu theo danh mục của đơn đã giao. Cột: 0 maDM, 1 tên danh mục, 2 doanh thu, 3 chiết khấu. */
    @Query("""
            select dm.maDM, dm.tenDanhMuc, coalesce(sum(ct.thanhTien), 0), coalesce(sum(ct.tienChietKhau), 0)
            from OrderItem ct join ct.donHangCuaHang d join ct.sach s join s.tuaSach ts join ts.danhMuc dm
            where d.trangThaiDonHang = :tt and d.ngayGiao >= :tu and d.ngayGiao < :den
            group by dm.maDM, dm.tenDanhMuc
            """)
    List<Object[]> sumByCategoryDeliveredBetween(@Param("tt") String trangThai,
                                                 @Param("tu") LocalDateTime tu, @Param("den") LocalDateTime den);
}
