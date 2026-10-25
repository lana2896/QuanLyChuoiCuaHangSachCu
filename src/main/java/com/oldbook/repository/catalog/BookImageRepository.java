package com.oldbook.repository.catalog;

import com.oldbook.entity.catalog.BookImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface BookImageRepository extends JpaRepository<BookImage, Integer> {
    List<BookImage> findAllBySach_MaSachInOrderBySach_MaSachAscThuTuAsc(Collection<Integer> maSachs);

    void deleteAllBySach_MaSach(Integer maSach);
}
