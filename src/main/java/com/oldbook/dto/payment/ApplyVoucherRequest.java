package com.oldbook.dto.payment;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ApplyVoucherRequest {

    @NotEmpty(message = "Vui lòng chọn ít nhất một sản phẩm để đặt hàng")
    private List<Integer> maCTGioHangs;

    @Size(max = 50, message = "Mã voucher tối đa 50 ký tự")
    private String code;

    private Integer maCH;
}
