package com.oldbook.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyRegisterOtpRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Mã OTP không được để trống")
    @Pattern(
            regexp = "^\\d{6}$",
            message = "Mã OTP phải gồm đúng 6 chữ số"
    )
    private String maOtp;
}