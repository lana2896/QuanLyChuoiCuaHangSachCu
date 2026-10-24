package com.oldbook.repository.admin;

import com.oldbook.entity.order.StoreOrder;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Truy vấn tổng hợp chỉ đọc cho báo cáo của admin.
 * Đặt riêng ở đây để không phải sửa repository của module order.
 */
public interface AdminReportRepository extends Repository<StoreOrder, Integer> {

    /**
     * Tổng theo ngày giao của các đơn cửa hàng đang ở một trạng thái, trong khoảng [tuLuc, denLuc).
     * Ngày không có đơn thì không có dòng.
     */
    @Query("""
            select cast(d.ngayGiao as LocalDate) as ngay,
                   count(d) as soDon,
                   sum(d.tienHangCH) as gmv,
                   sum(d.commissionTien) as hoaHong
            from StoreOrder d
            where d.trangThaiDonHang = :trangThai
              and d.ngayGiao >= :tuLuc
              and d.ngayGiao < :denLuc
            group by cast(d.ngayGiao as LocalDate)
            """)
    List<DailySales> sumShopOrdersByDeliveryDay(
            @Param("trangThai") String trangThai,
            @Param("tuLuc") LocalDateTime tuLuc,
            @Param("denLuc") LocalDateTime denLuc
    );

    interface DailySales {

        LocalDate getNgay();

        long getSoDon();

        BigDecimal getGmv();

        BigDecimal getHoaHong();
    }
}
