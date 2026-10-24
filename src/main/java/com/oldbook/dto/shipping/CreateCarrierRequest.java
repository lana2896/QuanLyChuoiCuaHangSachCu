package com.oldbook.dto.shipping;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCarrierRequest extends UpdateCarrierRequest {

    @NotBlank(message = "Mã đơn vị vận chuyển không được để trống")
    @Pattern(regexp = "^[A-Za-z0-9_-]{2,30}$",
            message = "Mã đơn vị vận chuyển chỉ gồm chữ, số, '-' hoặc '_' (2-30 ký tự)")
    private String maCode;
}
