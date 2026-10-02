package com.oldbook.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class LoginResponse {

    private Integer maND;

    private Integer maTK;

    private String vaiTro;

    // JWT chính thức sau khi đăng nhập/chọn vai trò
    private String token;

    // JWT tạm dùng để chọn vai trò
    private String roleSelectionToken;

    private boolean canChonVaiTro;

    private List<TaiKhoanResponse> taiKhoans;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class TaiKhoanResponse {

        private Integer maTK;

        private String vaiTro;

        private String trangThai;
    }
}