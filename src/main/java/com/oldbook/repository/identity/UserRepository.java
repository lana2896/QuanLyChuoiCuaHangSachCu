package com.oldbook.repository.identity;

import org.springframework.data.jpa.repository.JpaRepository;

import com.oldbook.entity.identity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

    // Tìm người dùng theo email đăng nhập
    Optional<User> findByEmail(String email);

    // Kiểm tra email đã tồn tại chưa
    boolean existsByEmail(String email);

    // Kiểm tra số điện thoại đã tồn tại chưa
    boolean existsBySoDienThoai(String soDienThoai);

    boolean existsBySoDienThoaiAndMaNDNot(
            String soDienThoai,
            Integer maND
    );
}