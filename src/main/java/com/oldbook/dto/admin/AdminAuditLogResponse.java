package com.oldbook.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class AdminAuditLogResponse {

    private Integer maNhatKy;

    private Integer maTK;

    private String vaiTro;

    private String tenNguoiThucHien;

    private String emailNguoiThucHien;

    private String hanhDong;

    private String loaiDoiTuong;

    private Integer maDoiTuong;

    private String moTa;

    private String diaChiIp;

    private LocalDateTime thoiGian;
}