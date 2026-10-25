package com.oldbook.dto.shipping;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangeCarrierStatusRequest {

    @NotBlank(message = "Trạng thái không được để trống")
    private String trangThai;

    @Size(max = 500, message = "Lý do tạm dừng tối đa 500 ký tự")
    private String lyDo;
}
