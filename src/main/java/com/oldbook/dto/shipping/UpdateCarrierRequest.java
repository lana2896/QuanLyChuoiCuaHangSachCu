package com.oldbook.dto.shipping;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UpdateCarrierRequest {

    @NotBlank(message = "Tên đơn vị không được để trống")
    @Size(max = 255, message = "Tên đơn vị tối đa 255 ký tự")
    private String tenDonVi;

    @NotNull(message = "Phí vận chuyển không được để trống")
    @PositiveOrZero(message = "Phí vận chuyển không được âm")
    @DecimalMax(value = "100000000", message = "Phí vận chuyển quá lớn")
    private BigDecimal phiVanChuyen;

    @Size(max = 255, message = "Tên người liên hệ tối đa 255 ký tự")
    private String nguoiLienHe;

    @Pattern(regexp = "^$|^[0-9+().\\s-]{8,20}$", message = "Số điện thoại không hợp lệ")
    private String soDienThoai;

    @Email(message = "Email liên hệ không hợp lệ")
    @Size(max = 255, message = "Email tối đa 255 ký tự")
    private String email;

    @Size(max = 500, message = "Địa chỉ tối đa 500 ký tự")
    private String diaChi;

    @Pattern(regexp = "^$|^https?://\\S+$", message = "Website phải bắt đầu bằng http:// hoặc https://")
    @Size(max = 255, message = "Website tối đa 255 ký tự")
    private String website;

    @Size(max = 1000, message = "Ghi chú tối đa 1000 ký tự")
    private String ghiChu;

    @Size(max = 500, message = "Thông tin tích hợp tối đa 500 ký tự")
    private String thongTinTichHop;
}
