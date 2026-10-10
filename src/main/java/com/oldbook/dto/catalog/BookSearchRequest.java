package com.oldbook.dto.catalog;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class BookSearchRequest {
    private String tuKhoa;
    private Integer maDM;
    private Integer maCH;
    private BigDecimal giaTu;
    private BigDecimal giaDen;
    private BigDecimal doMoiTu;
    private BigDecimal doMoiDen;
    private Boolean conHang;
    private int page = 0;
    private int size = 12;
    private String sort = "moi-nhat";
}
