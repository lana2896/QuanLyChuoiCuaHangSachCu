package com.oldbook.dto.shipping;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingCarrierResponse {
    private Integer maDVVC;
    private String maCode;
    private String tenDonVi;
    private BigDecimal phiVanChuyen;
    private Integer thoiGianGiaoDuKien;
}
