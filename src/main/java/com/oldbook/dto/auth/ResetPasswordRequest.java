package com.oldbook.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Mã OTP không được để trống")
    @Pattern(
            regexp = "^\\d{6}$",
            message = "Mã OTP phải gồm đúng 6 chữ số"
    )
    private String maOtp;

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(min = 8, message = "Mật khẩu phải có ít nhất 8 ký tự")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
            message = "Mật khẩu phải gồm ít nhất chữ và số"
    )
    private String matKhauMoi;

    @NotBlank(message = "Xác nhận mật khẩu mới không được để trống")
    private String xacNhanMatKhauMoi;
}