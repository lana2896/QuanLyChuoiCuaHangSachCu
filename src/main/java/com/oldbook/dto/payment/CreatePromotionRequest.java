package com.oldbook.dto.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePromotionRequest extends UpdatePromotionRequest {

    @NotBlank(message = "Mã voucher không được để trống")
    @Pattern(regexp = "^[A-Za-z0-9_-]{3,50}$",
            message = "Mã voucher chỉ gồm chữ, số, '-' hoặc '_' (3-50 ký tự)")
    private String code;
}
