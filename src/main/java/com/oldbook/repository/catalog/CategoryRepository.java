package com.oldbook.repository.catalog;

import com.oldbook.entity.catalog.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
}
