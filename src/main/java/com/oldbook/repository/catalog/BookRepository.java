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
            join fetch s.danhMuc dm
            where s.trangThaiDuyet = :duyet
              and s.trangThaiBan = :ban
              and ch.trangThaiDuyet = :duyet
            order by s.ngayTao desc, s.maSach desc
            """)
    List<Book> findPublic(@Param("duyet") String duyet, @Param("ban") String ban);

    @Override
    @EntityGraph(attributePaths = {"cuaHang", "danhMuc"})
    Page<Book> findAll(Specification<Book> specification, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"cuaHang", "danhMuc"})
    Optional<Book> findOne(Specification<Book> specification);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Book s where s.maSach = :maSach")
    Optional<Book> findByIdForUpdate(@Param("maSach") Integer maSach);
}