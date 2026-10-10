package com.oldbook.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderRequest {

    @NotNull(message = "Mã địa chỉ giao hàng không được để trống")
    private Integer maDiaChi;

    @NotBlank(message = "Phương thức thanh toán không được để trống")
    private String phuongThucTT;

    // Các dòng giỏ hàng (maCTGioHang) khách đã tích chọn để đặt hàng.
    @NotEmpty(message = "Vui lòng chọn ít nhất một sản phẩm để đặt hàng")
    private List<Integer> maCTGioHangs;

    // Mỗi cửa hàng trong giỏ hàng có đơn vị vận chuyển + ghi chú riêng.
    @NotEmpty(message = "Vui lòng chọn đơn vị vận chuyển cho từng cửa hàng")
    @Valid
    private List<StoreOrderOptionRequest> storeOrders;
}