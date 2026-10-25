package com.oldbook.repository.catalog;

import com.oldbook.entity.catalog.BookTitle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface BookTitleRepository extends JpaRepository<BookTitle, Integer>, JpaSpecificationExecutor<BookTitle> {
    List<BookTitle> findAllByDanhMuc_MaDMAndTrangThaiBanAndDanhMuc_TrangThaiBanOrderByTenSachAsc(
            Integer maDM, String trangThaiBan, String trangThaiDanhMuc);

    @Override
    @EntityGraph(attributePaths = "danhMuc")
    Page<BookTitle> findAll(Specification<BookTitle> specification, Pageable pageable);
}
