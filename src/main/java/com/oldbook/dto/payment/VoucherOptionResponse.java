package com.oldbook.dto.payment;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class VoucherOptionResponse {
    private Integer maCH;
    private String tenCuaHang;
    private String code;
    private String tenKhuyenMai;
    private String loaiGiamGia;
    private BigDecimal giaTriGiam;
    private BigDecimal giaTriGiamToiDa;
    private BigDecimal giaTriDonToiThieu;
    private LocalDateTime ngayKetThuc;
    private Integer conLai;
    private boolean daDuDieuKien;
    private BigDecimal giamDuKien;
    private String lyDoKhongApDung;
}
