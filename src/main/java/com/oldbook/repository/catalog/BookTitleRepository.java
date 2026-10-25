package com.oldbook.repository.catalog;

import com.oldbook.entity.catalog.BookTitle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookTitleRepository extends JpaRepository<BookTitle, Integer>, JpaSpecificationExecutor<BookTitle> {
    List<BookTitle> findAllByDanhMuc_MaDMAndTrangThaiBanAndDanhMuc_TrangThaiBanOrderByTenSachAsc(
            Integer maDM, String trangThaiBan, String trangThaiDanhMuc);

    @Modifying(flushAutomatically = true)
    @Query("update BookTitle t set t.trangThaiBan = :status where t.danhMuc.maDM = :maDM")
    int updateSaleStatusByCategory(@Param("maDM") Integer maDM, @Param("status") String status);

    @Override
    @EntityGraph(attributePaths = "danhMuc")
    Page<BookTitle> findAll(Specification<BookTitle> specification, Pageable pageable);
}
