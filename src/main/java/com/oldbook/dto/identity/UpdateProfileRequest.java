package com.oldbook.dto.identity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequest {

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 255, message = "Họ tên không được vượt quá 255 ký tự")
    private String hoTen;

    @Pattern(
            regexp = "^$|^0\\d{9,10}$",
            message = "Số điện thoại không hợp lệ"
    )
    private String soDienThoai;

    @Size(
            max = 500,
            message = "URL avatar không được vượt quá 500 ký tự"
    )
    private String avatarUrl;

    @Size(
            max = 1000,
            message = "Tiểu sử không được vượt quá 1000 ký tự"
    )
    private String tieuSu;
}