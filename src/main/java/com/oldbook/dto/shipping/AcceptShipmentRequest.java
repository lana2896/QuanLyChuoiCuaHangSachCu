package com.oldbook.dto.shipping;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Đơn vị vận chuyển tiếp nhận đơn (POST /api/shipping/{maCode}/requests).
 * Hãng lấy từ {maCode} trên URL; mã vận đơn đã cấp khi cửa hàng xác nhận nên không gửi lại.
 */
@Getter
@Setter
public class AcceptShipmentRequest {
    @NotNull
    private Integer maDHCH;

    @Size(max = 1000)
    private String ghiChuGiaoHang;
}
