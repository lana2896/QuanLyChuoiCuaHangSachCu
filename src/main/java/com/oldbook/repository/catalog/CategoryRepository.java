package com.oldbook.repository.catalog;

import com.oldbook.entity.catalog.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
    List<Category> findAllByTrangThaiBanOrderByTenDanhMucAsc(String trangThaiBan);

    boolean existsByTenDanhMucIgnoreCase(String tenDanhMuc);

    boolean existsByTenDanhMucIgnoreCaseAndMaDMNot(String tenDanhMuc, Integer maDM);
}
