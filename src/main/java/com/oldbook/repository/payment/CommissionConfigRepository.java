package com.oldbook.repository.payment;

import com.oldbook.entity.payment.CommissionConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CommissionConfigRepository extends JpaRepository<CommissionConfig, Integer> {

    /** Cấu hình chiết khấu đang hiệu lực của một danh mục, mới nhất trước. */
    @Query("""
            select c from CommissionConfig c
            where c.danhMuc.maDM = :maDM
              and c.trangThai = :trangThai
              and (c.ngayBatDau is null or c.ngayBatDau <= :now)
              and (c.ngayKetThuc is null or c.ngayKetThuc >= :now)
            order by c.ngayBatDau desc, c.maCauHinhChietKhau desc
            """)
    List<CommissionConfig> findEffective(@Param("maDM") Integer maDM,
                                         @Param("trangThai") String trangThai,
                                         @Param("now") LocalDateTime now);

    /** Lịch sử tỷ lệ hoa hồng của một danh mục, mới nhất trước. */
    List<CommissionConfig> findByDanhMuc_MaDMOrderByNgayBatDauDescMaCauHinhChietKhauDesc(Integer maDM);
}
