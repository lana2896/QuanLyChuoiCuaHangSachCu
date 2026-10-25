package com.oldbook.repository.catalog;

import com.oldbook.entity.catalog.BookTitle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface BookTitleRepository extends JpaRepository<BookTitle, Integer>, JpaSpecificationExecutor<BookTitle> {
    List<BookTitle> findAllByDanhMuc_MaDMOrderByTenSachAsc(Integer maDM);
}
