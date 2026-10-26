package com.oldbook.dto.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Tài khoản ngân hàng khách muốn nhận tiền hoàn. */
public record BankAccountRequest(
        @NotBlank(message = "Vui lòng chọn ngân hàng")
        @Pattern(regexp = "[0-9]{6}", message = "Mã ngân hàng không hợp lệ")
        String maNganHang,

        @NotBlank(message = "Số tài khoản không được để trống")
        @Pattern(regexp = "[0-9A-Za-z]{4,30}", message = "Số tài khoản chỉ gồm chữ và số, từ 4 đến 30 ký tự")
        String soTaiKhoan,

        @NotBlank(message = "Tên chủ tài khoản không được để trống")
        @Size(max = 255, message = "Tên chủ tài khoản không được vượt quá 255 ký tự")
        String tenChuTaiKhoan
) {
}
