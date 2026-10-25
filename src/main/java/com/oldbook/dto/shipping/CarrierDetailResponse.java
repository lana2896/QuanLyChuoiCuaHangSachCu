package com.oldbook.dto.shipping;

import com.oldbook.entity.shipping.ShippingCarrier;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class CarrierDetailResponse {
    private Integer maDVVC;
    private String maCode;
    private String tenDonVi;
    private BigDecimal phiVanChuyen;
    private Integer thoiGianGiaoDuKien;
    private String nguoiLienHe;
    private String soDienThoai;
    private String email;
    private String diaChi;
    private String website;
    private String ghiChu;
    private String thongTinTichHop;
    private String trangThai;
    private String lyDoTamDung;
    private LocalDateTime ngayCapNhatTrangThai;
    private LocalDateTime ngayCapNhat;
    private LocalDate ngayTao;
    private String duongDanTrang;
    private String duongDanApi;
    private String duongDanTrangDayDu;
    private String duongDanApiDayDu;
    private long soVanDon;
    private long soVanDonDangXuLy;
    private long soVanDonDaGiao;

    public static CarrierDetailResponse from(ShippingCarrier e, String baseUrl,
                                                   long soVanDon, long soVanDonDangXuLy, long soVanDonDaGiao) {
        String trang = e.getMaCode() == null ? null : CarrierResponse.pageUrl(e.getMaCode());
        String api = e.getMaCode() == null ? null : CarrierResponse.apiUrl(e.getMaCode());
        String goc = baseUrl == null ? "" : baseUrl.replaceAll("/+$", "");
        return CarrierDetailResponse.builder()
                .maDVVC(e.getMaDVVC())
                .maCode(e.getMaCode())
                .tenDonVi(e.getTenDonVi())
                .phiVanChuyen(e.getPhiVanChuyen())
                .thoiGianGiaoDuKien(e.getThoiGianGiaoDuKien())
                .nguoiLienHe(e.getNguoiLienHe())
                .soDienThoai(e.getSoDienThoai())
                .email(e.getEmail())
                .diaChi(e.getDiaChi())
                .website(e.getWebsite())
                .ghiChu(e.getGhiChu())
                .thongTinTichHop(e.getThongTinTichHop())
                .trangThai(e.getTrangThai())
                .lyDoTamDung(e.getLyDoTamDung())
                .ngayCapNhatTrangThai(e.getNgayCapNhatTrangThai())
                .ngayCapNhat(e.getNgayCapNhat())
                .ngayTao(e.getNgayTao())
                .duongDanTrang(trang)
                .duongDanApi(api)
                .duongDanTrangDayDu(trang == null ? null : goc + trang)
                .duongDanApiDayDu(api == null ? null : goc + api)
                .soVanDon(soVanDon)
                .soVanDonDangXuLy(soVanDonDangXuLy)
                .soVanDonDaGiao(soVanDonDaGiao)
                .build();
    }
}
