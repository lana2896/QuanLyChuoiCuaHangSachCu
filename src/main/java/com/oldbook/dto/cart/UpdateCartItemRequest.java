package com.oldbook.dto.cart;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCartItemRequest {

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng phải lớn hơn hoặc bằng 1")
    @Max(value = 999, message = "Số lượng không hợp lệ")
    private Integer soLuong;
}
