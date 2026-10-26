package com.oldbook.dto.identity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AddressResponse {

    private Integer maDiaChi;
    private String tenNguoiNhan;
    private String soDienThoai;
    private Integer maTinh;
    private Integer maXa;
    private String tinhThanh;
    private String quanHuyen;
    private String phuongXa;
    private String diaChiChiTiet;
    private Boolean laMacDinh;
}