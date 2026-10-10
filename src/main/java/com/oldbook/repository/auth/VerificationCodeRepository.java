package com.oldbook.repository.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import com.oldbook.entity.auth.VerificationCode;

import java.util.Optional;

public interface VerificationCodeRepository
        extends JpaRepository<VerificationCode, Integer> {

    Optional<VerificationCode> findTopByNguoiDung_MaNDAndLoaiXacThucAndDaSuDungFalseOrderByNgayTaoDesc(
            Integer maND,
            String loaiXacThuc
    );
}