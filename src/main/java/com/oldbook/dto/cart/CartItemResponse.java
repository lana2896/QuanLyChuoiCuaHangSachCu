package com.oldbook.dto.cart;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
public class CartItemResponse {

    private Integer maCTGioHang;

    private Integer maSach;

    private String tenSach;

    private String tacGia;

    private String hinhAnhUrl;

    private Integer maCH;

    private String tenCuaHang;

    private BigDecimal donGia;

    private Integer soLuong;

    private Integer soLuongTon;

    private BigDecimal thanhTien;

    private String canhBao;

    private boolean coTheMua;
}
