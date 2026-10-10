package com.oldbook.repository.system;

import org.springframework.data.jpa.repository.JpaRepository;

import com.oldbook.entity.system.NhatKyHeThong;

import java.util.Optional;

public interface NhatKyHeThongRepository
        extends JpaRepository<NhatKyHeThong, Integer> {

    Optional<NhatKyHeThong> findFirstByLoaiDoiTuongAndMaDoiTuongAndHanhDongOrderByThoiGianDescMaNhatKyDesc(
            String loaiDoiTuong, Integer maDoiTuong, String hanhDong);
}
