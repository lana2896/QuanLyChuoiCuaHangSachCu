package com.oldbook.dto.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PromotionStatusRequest {

    @NotBlank(message = "Trạng thái không được để trống")
    @Pattern(regexp = "^(HOAT_DONG|TAM_DUNG)$", message = "Trạng thái voucher không hợp lệ")
    private String trangThai;
}
