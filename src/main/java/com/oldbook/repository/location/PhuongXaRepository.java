package com.oldbook.repository.location;

import com.oldbook.entity.location.PhuongXa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PhuongXaRepository extends JpaRepository<PhuongXa, Integer> {
    List<PhuongXa> findAllByTinh_MaTinhOrderByTenXaAsc(Integer maTinh);

    Optional<PhuongXa> findByMaXaAndTinh_MaTinh(Integer maXa, Integer maTinh);
}
