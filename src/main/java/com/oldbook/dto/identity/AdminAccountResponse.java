package com.oldbook.dto.identity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class AdminAccountResponse {

    private Integer maTK;
    private Integer maND;

    private String email;
    private String hoTen;
    private String soDienThoai;

    private String vaiTro;
    private String trangThai;

    private LocalDateTime ngayTao;
}