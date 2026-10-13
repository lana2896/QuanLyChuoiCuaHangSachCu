package com.oldbook.dto.shipping;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** Sửa thông tin đối tác. Không có maCode: đổi mã sẽ làm hỏng link và API mà hãng đang dùng. */
@Getter
@Setter
public class UpdateCarrierRequest {

    @NotBlank(message = "Tên đơn vị không được để trống")
    @Size(max = 255)
    private String tenDonVi;

    @NotNull(message = "Phí vận chuyển không được để trống")
    @PositiveOrZero
    private BigDecimal phiVanChuyen;

    @NotNull(message = "Thời gian giao dự kiến không được để trống")
    @Positive
    private Integer thoiGianGiaoDuKien;

    @Size(max = 20)
    private String soDienThoai;

    @Email
    @Size(max = 255)
    private String email;

    @Size(max = 500)
    private String thongTinTichHop;
}
