package com.oldbook.dto.shipping;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Cập nhật trạng thái vận đơn từ cổng đối tác.
 * Số lần giao thất bại do hệ thống tự đếm; tiền COD/hoàn kho do OrderSettlementService tự chốt.
 */
@Getter
@Setter
public class UpdateShipmentRequest {
    /** Một giá trị của ShipmentStatus, ví dụ DA_LAY_HANG, DANG_GIAO, DA_GIAO, GIAO_THAT_BAI. DA_HOAN_TRA do hệ thống tự chuyển. */
    @NotBlank
    @Size(max = 50)
    private String trangThai;

    @Size(max = 1000)
    private String ghiChu;
}
