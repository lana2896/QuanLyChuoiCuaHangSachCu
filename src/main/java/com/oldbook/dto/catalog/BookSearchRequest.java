package com.oldbook.dto.catalog;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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
    private List<String> tinhTrang = new ArrayList<>();
    private String khoangGia;
    private Boolean conHang;
    private int page = 0;
    private int size = 12;
    private String sort = "moi-nhat";
}
