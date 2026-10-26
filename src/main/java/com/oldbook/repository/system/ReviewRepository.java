package com.oldbook.repository.system;

import com.oldbook.entity.system.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {
    @EntityGraph(attributePaths = {"nguoiDung"})
    @Query(value = """
            select dg from Review dg
            where dg.chiTietDonHang.sach.maSach = :maSach
              and dg.soSao between 1 and 5
            order by dg.ngayDanhGia desc, dg.maDanhGia desc
            """,
            countQuery = """
            select count(dg) from Review dg
            where dg.chiTietDonHang.sach.maSach = :maSach
              and dg.soSao between 1 and 5
            """)
    Page<Review> findByBookId(@Param("maSach") Integer maSach, Pageable pageable);

    boolean existsByChiTietDonHang_MaCTDH(Integer maCTDH);

    @EntityGraph(attributePaths = {"chiTietDonHang", "nguoiDung", "media"})
    List<Review> findAllByChiTietDonHang_DonHangCuaHang_MaDHCHIn(List<Integer> maDHCH);
}
