package com.oldbook.dto.identity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import com.oldbook.dto.auth.LoginResponse;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ProfileResponse {

    private Integer maND;

    private String email;

    private String hoTen;

    private String soDienThoai;

    private String avatarUrl;

    private String tieuSu;

    // Mọi tài khoản (vai trò) của người dùng, để header biết có thể chuyển sang tài khoản nào.
    private List<LoginResponse.AccountResponse> taiKhoans;
}