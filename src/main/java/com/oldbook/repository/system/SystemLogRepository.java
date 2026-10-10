package com.oldbook.repository.system;

import org.springframework.data.jpa.repository.JpaRepository;

import com.oldbook.entity.system.SystemLog;

import java.util.Optional;

public interface SystemLogRepository
        extends JpaRepository<SystemLog, Integer> {

    Optional<SystemLog> findFirstByLoaiDoiTuongAndMaDoiTuongAndHanhDongOrderByThoiGianDescMaNhatKyDesc(
            String loaiDoiTuong, Integer maDoiTuong, String hanhDong);
}
