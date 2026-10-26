package com.oldbook.dto.payment;

import com.oldbook.constant.payment.PromotionStatus;
import com.oldbook.entity.payment.Promotion;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class PromotionResponse {

    public static final String DISPLAY_ACTIVE = "DANG_AP_DUNG";
    public static final String DISPLAY_PAUSED = "TAM_DUNG";
    public static final String DISPLAY_UPCOMING = "CHUA_BAT_DAU";
    public static final String DISPLAY_EXPIRED = "HET_HAN";
    public static final String DISPLAY_EXHAUSTED = "HET_LUOT";

    private Integer maKM;
    private String code;
    private String tenKhuyenMai;
    private String loaiVoucher;
    private String loaiGiamGia;
    private BigDecimal giaTriGiam;
    private BigDecimal giaTriGiamToiDa;
    private BigDecimal giaTriDonToiThieu;
    private Integer soLuong;
    private Integer daSuDung;
    private Integer conLai;
    private LocalDateTime ngayBatDau;
    private LocalDateTime ngayKetThuc;
    private String trangThai;
    private String trangThaiHienThi;
    private boolean coTheXoa;

    public static String displayStatus(Promotion p, LocalDateTime now) {
        if (PromotionStatus.TAM_DUNG.name().equals(p.getTrangThai())) {
            return DISPLAY_PAUSED;
        }
        if (p.getNgayBatDau() != null && now.isBefore(p.getNgayBatDau())) {
            return DISPLAY_UPCOMING;
        }
        if (p.getNgayKetThuc() != null && now.isAfter(p.getNgayKetThuc())) {
            return DISPLAY_EXPIRED;
        }
        if (used(p) >= (p.getSoLuong() == null ? 0 : p.getSoLuong())) {
            return DISPLAY_EXHAUSTED;
        }
        return DISPLAY_ACTIVE;
    }

    public static PromotionResponse from(Promotion p, LocalDateTime now, boolean daCoLuotDung) {
        int soLuong = p.getSoLuong() == null ? 0 : p.getSoLuong();
        return PromotionResponse.builder()
                .maKM(p.getMaKM())
                .code(p.getCode())
                .tenKhuyenMai(p.getTenKhuyenMai())
                .loaiVoucher(p.getLoaiVoucher())
                .loaiGiamGia(p.getLoaiGiamGia())
                .giaTriGiam(p.getGiaTriGiam())
                .giaTriGiamToiDa(p.getGiaTriGiamToiDa())
                .giaTriDonToiThieu(p.getGiaTriDonToiThieu())
                .soLuong(soLuong)
                .daSuDung(used(p))
                .conLai(Math.max(0, soLuong - used(p)))
                .ngayBatDau(p.getNgayBatDau())
                .ngayKetThuc(p.getNgayKetThuc())
                .trangThai(p.getTrangThai())
                .trangThaiHienThi(displayStatus(p, now))
                .coTheXoa(!daCoLuotDung)
                .build();
    }

    private static int used(Promotion p) {
        return p.getDaSuDung() == null ? 0 : p.getDaSuDung();
    }
}
