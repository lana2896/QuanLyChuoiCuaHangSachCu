package com.oldbook.repository.location;

import com.oldbook.entity.location.TinhThanh;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TinhThanhRepository extends JpaRepository<TinhThanh, Integer> {
    boolean existsByTenTinh(String tenTinh);

    List<TinhThanh> findAllByOrderByTenTinhAsc();
}
