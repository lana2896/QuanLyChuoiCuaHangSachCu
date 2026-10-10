package com.oldbook.repository.identity;

import org.springframework.data.jpa.repository.JpaRepository;

import com.oldbook.entity.identity.Account;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Integer> {

    List<Account> findByNguoiDung_MaND(Integer maND);

    Optional<Account> findByNguoiDung_MaNDAndVaiTro(
            Integer maND,
            String vaiTro
    );

    boolean existsByNguoiDung_MaNDAndVaiTro(
            Integer maND,
            String vaiTro
    );

    boolean existsByNguoiDung_MaNDAndVaiTroAndMaTKNot(
            Integer maND,
            String vaiTro,
            Integer maTK
    );

    List<Account> findByNguoiDung_MaNDAndTrangThai(
            Integer maND,
            String trangThai
    );
}