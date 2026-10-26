package com.oldbook.repository.notification;

import com.oldbook.entity.identity.Account;
import com.oldbook.entity.notification.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    Page<Notification> findByTaiKhoanNhan_MaTKOrderByNgayTaoDescMaThongBaoDesc(
            Integer maTK,
            Pageable pageable
    );

    // Lần nhắc gần nhất cùng loại cho một người (mọi tài khoản của người đó), để giới hạn tần suất nhắc.
    Optional<Notification> findFirstByLoaiAndTaiKhoanNhan_NguoiDung_MaNDOrderByNgayTaoDesc(String loai, Integer maND);

    long countByTaiKhoanNhan_MaTKAndDaDocFalse(Integer maTK);

    // Tìm kèm mã tài khoản nhận để không ai đọc hay sửa được thông báo của người khác.
    Optional<Notification> findByMaThongBaoAndTaiKhoanNhan_MaTK(
            Integer maThongBao,
            Integer maTK
    );

    @Modifying
    @Query("""
            update Notification t
            set t.daDoc = true, t.ngayDoc = :luc
            where t.taiKhoanNhan.maTK = :maTK
              and t.daDoc = false
            """)
    int markAllRead(
            @Param("maTK") Integer maTK,
            @Param("luc") LocalDateTime luc
    );

    // Danh sách người nhận theo vai trò. Đặt ở đây để không phải sửa AccountRepository của module identity.
    @Query("""
            select t
            from Account t
            where t.vaiTro = :vaiTro
              and t.trangThai = :trangThai
            """)
    List<Account> findAccountsByRoleAndStatus(
            @Param("vaiTro") String vaiTro,
            @Param("trangThai") String trangThai
    );
}
