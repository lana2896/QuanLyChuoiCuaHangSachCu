package com.oldbook.dto.admin;

import com.oldbook.dto.identity.AdminAccountResponse;
import com.oldbook.dto.identity.AddressResponse;
import com.oldbook.dto.store.StoreResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class AdminUserProfileResponse {

    private Integer maND;

    private String email;

    private String hoTen;

    private String soDienThoai;

    private String avatarUrl;

    private String tieuSu;

    private LocalDateTime ngayTao;

    private List<AdminAccountResponse> taiKhoans;

    private List<AddressResponse> diaChis;

    private StoreResponse cuaHang;

    private List<AdminAuditLogResponse> hoatDongGanDay;
}
