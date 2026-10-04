package com.oldbook.dto.cart;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class CartResponse {

    private Integer maGioHang;

    private List<CartItemResponse> items;

    private int soMuc;

    private int tongSoLuong;

    private BigDecimal tongTien;
}
