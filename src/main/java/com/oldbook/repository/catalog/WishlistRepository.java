package com.oldbook.repository.catalog;

import com.oldbook.entity.catalog.UserBookKey;
import com.oldbook.entity.catalog.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WishlistRepository extends JpaRepository<Wishlist, UserBookKey> {

    @Query("""
            select w from Wishlist w
            join fetch w.sach s
            join fetch s.cuaHang ch
            join fetch s.tuaSach ts
            join fetch ts.danhMuc dm
            where w.id.maND = :maND
              and s.trangThaiDuyet = :duyet
              and s.trangThaiBan = :ban
              and ch.trangThaiDuyet = :duyet
            order by w.ngayThem desc, w.id.maSach desc
            """)
    List<Wishlist> findPublicByUserId(@Param("maND") Integer maND,
                                      @Param("duyet") String duyet,
                                      @Param("ban") String ban);
}
