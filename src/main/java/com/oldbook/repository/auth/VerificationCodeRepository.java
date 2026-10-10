package com.oldbook.repository.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import com.oldbook.entity.auth.MaXacThuc;

import java.util.Optional;

public interface MaXacThucRepository
        extends JpaRepository<MaXacThuc, Integer> {

    Optional<MaXacThuc> findTopByNguoiDung_MaNDAndLoaiXacThucAndDaSuDungFalseOrderByNgayTaoDesc(
            Integer maND,
            String loaiXacThuc
    );
}