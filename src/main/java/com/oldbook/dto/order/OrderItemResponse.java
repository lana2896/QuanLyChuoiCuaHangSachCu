package com.oldbook.dto.order;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResponse {
    private Integer maCTDH;
    private Integer maSach;
    private String tenSach;
    private Integer soLuong;
    private BigDecimal donGia;
    private BigDecimal thanhTien;
}
