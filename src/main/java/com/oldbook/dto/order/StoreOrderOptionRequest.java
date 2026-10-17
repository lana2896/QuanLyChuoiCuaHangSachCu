package com.oldbook.dto.order;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/** Lựa chọn của khách cho riêng từng cửa hàng trong giỏ hàng: đơn vị vận chuyển + ghi chú. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreOrderOptionRequest {

    @NotNull(message = "Mã cửa hàng không được để trống")
    private Integer maCH;

    @NotNull(message = "Vui lòng chọn đơn vị vận chuyển cho từng cửa hàng")
    private Integer maDVVC;

    @Size(max = 1000, message = "Ghi chú không được vượt quá 1000 ký tự")
    private String ghiChu;
}
