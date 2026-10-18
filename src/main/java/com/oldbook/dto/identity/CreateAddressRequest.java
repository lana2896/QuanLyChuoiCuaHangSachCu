package com.oldbook.dto.identity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateAddressRequest {

    @NotBlank(message = "Họ tên người nhận không được để trống")
    @Size(max = 255, message = "Họ tên người nhận không được vượt quá 255 ký tự")
    private String tenNguoiNhan;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(
            regexp = "^(0|\\+84)[0-9]{8,10}$",
            message = "Số điện thoại không hợp lệ"
    )
    private String soDienThoai;

    @NotBlank(message = "Tỉnh/Thành phố không được để trống")
    @Size(max = 100, message = "Tỉnh/Thành phố không được vượt quá 100 ký tự")
    private String tinhThanh;

    @NotBlank(message = "Quận/Huyện không được để trống")
    @Size(max = 100, message = "Quận/Huyện không được vượt quá 100 ký tự")
    private String quanHuyen;

    @NotBlank(message = "Phường/Xã không được để trống")
    @Size(max = 100, message = "Phường/Xã không được vượt quá 100 ký tự")
    private String phuongXa;

    @NotBlank(message = "Địa chỉ chi tiết không được để trống")
    @Size(max = 500, message = "Địa chỉ chi tiết không được vượt quá 500 ký tự")
    private String diaChiChiTiet;

    private Boolean laMacDinh;
}