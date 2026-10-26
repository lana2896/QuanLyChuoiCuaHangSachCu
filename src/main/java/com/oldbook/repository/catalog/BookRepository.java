package com.oldbook.repository.catalog;

import com.oldbook.entity.catalog.Book;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Integer>, JpaSpecificationExecutor<Book> {
    @Query("""
            select s from Book s
            join fetch s.cuaHang ch
            join fetch s.tuaSach ts
            join fetch ts.danhMuc dm
            where s.trangThaiDuyet = :duyet
              and s.trangThaiBan = :ban
              and ch.trangThaiDuyet = :duyet
              and ts.trangThaiBan = :ban
              and dm.trangThaiBan = :ban
            order by s.ngayTao desc, s.maSach desc
            """)
    List<Book> findPublic(@Param("duyet") String duyet, @Param("ban") String ban);

    @Override
    @EntityGraph(attributePaths = {"cuaHang", "tuaSach", "tuaSach.danhMuc"})
    Page<Book> findAll(Specification<Book> specification, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"cuaHang", "tuaSach", "tuaSach.danhMuc"})
    Optional<Book> findOne(Specification<Book> specification);

    List<Book> findAllByTuaSach_MaTuaSach(Integer maTuaSach);

    /**
     * Theo từng chi nhánh: số bản sách đang bán công khai và tổng số cuốn tồn của các bản đó.
     * Mỗi dòng: {maCH, số bản sách, tổng tồn}.
     */
    @Query("""
            select s.cuaHang.maCH, count(s), coalesce(sum(s.soLuongTon), 0) from Book s
            where s.trangThaiDuyet = :duyet
              and s.trangThaiBan = :ban
              and s.tuaSach.trangThaiBan = :ban
              and s.tuaSach.danhMuc.trangThaiBan = :ban
            group by s.cuaHang.maCH
            """)
    List<Object[]> summarizePublicStockByStore(@Param("duyet") String duyet, @Param("ban") String ban);

    /** Các bản sách cùng tựa đã duyệt ở một chi nhánh: dùng để gộp tồn kho khi nhận điều chuyển. */
    @Query("""
            select s from Book s
            where s.cuaHang.maCH = :maCH
              and s.tuaSach.maTuaSach = :maTuaSach
              and s.trangThaiDuyet = :duyet
            order by s.maSach asc
            """)
    List<Book> findApprovedCopiesAtStore(@Param("maCH") Integer maCH,
                                         @Param("maTuaSach") Integer maTuaSach,
                                         @Param("duyet") String duyet);

    /** Tồn kho toàn chuỗi: các bản sách đã duyệt còn hàng, lọc theo tên sách hoặc tác giả. */
    @EntityGraph(attributePaths = {"cuaHang", "tuaSach"})
    @Query("""
            select s from Book s
            where s.trangThaiDuyet = :duyet
              and s.soLuongTon > 0
              and (lower(s.tuaSach.tenSach) like :pattern or lower(s.tuaSach.tacGia) like :pattern)
            order by s.tuaSach.tenSach asc, s.maSach asc
            """)
    List<Book> findApprovedInStockByKeyword(@Param("duyet") String duyet,
                                            @Param("pattern") String pattern,
                                            Pageable pageable);

    @Modifying(flushAutomatically = true)
    @Query("update Book s set s.trangThaiBan = :status where s.tuaSach.maTuaSach = :maTuaSach")
    int updateSaleStatusByTitle(@Param("maTuaSach") Integer maTuaSach, @Param("status") String status);

    @Modifying(flushAutomatically = true)
    @Query("""
            update Book s set s.trangThaiBan = :status
            where s.tuaSach.maTuaSach in (
                select t.maTuaSach from BookTitle t where t.danhMuc.maDM = :maDM)
            """)
    int updateSaleStatusByCategory(@Param("maDM") Integer maDM, @Param("status") String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Book s where s.maSach = :maSach")
    Optional<Book> findByIdForUpdate(@Param("maSach") Integer maSach);
}