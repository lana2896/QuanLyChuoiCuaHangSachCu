package com.oldbook.repository.admin;

import com.oldbook.entity.identity.Account;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;

/**
 * Truy vấn tổng hợp chỉ đọc cho dashboard admin.
 * Đặt riêng ở đây để không phải sửa AccountRepository của module identity.
 */
public interface AdminDashboardRepository extends Repository<Account, Integer> {

    @Query("""
            select t.trangThai as code, count(t) as total
            from Account t
            group by t.trangThai
            """)
    List<CodeCount> countAccountsGroupedByStatus();

    @Query("""
            select t.vaiTro as code, count(t) as total
            from Account t
            group by t.vaiTro
            """)
    List<CodeCount> countAccountsGroupedByRole();

    interface CodeCount {

        String getCode();

        long getTotal();
    }
}
