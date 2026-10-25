package com.oldbook.repository.catalog;

import com.oldbook.entity.catalog.RecentlyViewed;
import com.oldbook.entity.catalog.UserBookKey;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecentlyViewedRepository extends JpaRepository<RecentlyViewed, UserBookKey> {

    @Query("""
            select x from RecentlyViewed x
            join fetch x.sach s
            join fetch s.cuaHang ch
            join fetch s.tuaSach ts
            join fetch ts.danhMuc dm
            where x.id.maND = :maND
              and s.trangThaiDuyet = :duyet
              and s.trangThaiBan = :ban
              and ch.trangThaiDuyet = :duyet
            order by x.lanXemGanNhat desc, x.id.maSach desc
            """)
    List<RecentlyViewed> findPublicByUserId(@Param("maND") Integer maND,
                                            @Param("duyet") String duyet,
                                            @Param("ban") String ban,
                                            Pageable pageable);
}
