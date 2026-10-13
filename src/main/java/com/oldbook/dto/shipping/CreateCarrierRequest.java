package com.oldbook.dto.shipping;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/**
 * Quản lý thêm đối tác vận chuyển mới = các trường của UpdateCarrierRequest + maCode.
 * maCode là một phần của URL (/shipping/{maCode}, /api/shipping/{maCode}) nên không đổi được sau khi tạo.
 */
@Getter
@Setter
public class CreateCarrierRequest extends UpdateCarrierRequest {

    @NotBlank(message = "Mã đơn vị vận chuyển không được để trống")
    @Pattern(regexp = "^[A-Za-z0-9_-]{2,30}$",
            message = "Mã đơn vị vận chuyển chỉ gồm chữ, số, '-' hoặc '_' (2-30 ký tự)")
    private String maCode;
}
