package com.oldbook.repository.catalog;

import com.oldbook.entity.catalog.Sach;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SachRepository extends JpaRepository<Sach, Integer>, JpaSpecificationExecutor<Sach> {
    @Query("""
            select s from Sach s
            join fetch s.cuaHang ch
            join fetch s.danhMuc dm
            where s.trangThaiDuyet = :duyet
              and s.trangThaiBan = :ban
              and ch.trangThaiDuyet = :duyet
            order by s.ngayTao desc, s.maSach desc
            """)
    List<Sach> findCongKhai(@Param("duyet") String duyet, @Param("ban") String ban);

    @Override
    @EntityGraph(attributePaths = {"cuaHang", "danhMuc"})
    Page<Sach> findAll(Specification<Sach> specification, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"cuaHang", "danhMuc"})
    Optional<Sach> findOne(Specification<Sach> specification);

}
