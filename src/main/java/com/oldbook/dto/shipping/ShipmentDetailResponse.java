package com.oldbook.dto.shipping;

import com.oldbook.constant.shipping.ShipmentStatus;
import com.oldbook.entity.shipping.Shipment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Getter
@Builder
public class ShipmentDetailResponse {
    private Integer maVanChuyen;
    private Integer maDHCH;
    private Integer maDH;
    private Integer maDVVC;
    private String maCode;
    private String tenDonVi;
    private String maVanDon;
    private String trangThaiVanChuyen;
    private String tenTrangThai;
    private String trangThaiDonHang;
    private Integer soLanGiaoThatBai;
    /** Số lần giao thất bại tối đa; đạt tới số này thì hệ thống tự hoàn hàng. */
    private Integer soLanGiaoToiDa;
    private String ghiChuGiaoHang;
    private LocalDateTime ngayCapNhat;
    private String lyDoHuy;

    /** Các trạng thái được phép chuyển tới (lấy từ ShipmentStatus.canMoveTo); giao diện không tự giữ bảng riêng. */
    private List<String> trangThaiTiepTheo;

    public static ShipmentDetailResponse from(Shipment e) {
        return from(e, null);
    }

    public static ShipmentDetailResponse from(Shipment e, Integer soLanGiaoToiDa) {
        ShipmentStatus st = ShipmentStatus.fromName(e.getTrangThaiVanChuyen());
        return ShipmentDetailResponse.builder()
                .maVanChuyen(e.getMaVanChuyen())
                .maDHCH(e.getDonHangCuaHang().getMaDHCH())
                .maDH(e.getDonHangCuaHang().getDonHang().getMaDH())
                .maDVVC(e.getDonViVanChuyen().getMaDVVC())
                .maCode(e.getDonViVanChuyen().getMaCode())
                .tenDonVi(e.getDonViVanChuyen().getTenDonVi())
                .maVanDon(e.getMaVanDon())
                .trangThaiVanChuyen(e.getTrangThaiVanChuyen())
                .tenTrangThai(st == null ? e.getTrangThaiVanChuyen() : st.getDisplayName())
                .trangThaiDonHang(e.getDonHangCuaHang().getTrangThaiDonHang())
                .soLanGiaoThatBai(e.getSoLanGiaoThatBai() == null ? 0 : e.getSoLanGiaoThatBai())
                .soLanGiaoToiDa(soLanGiaoToiDa)
                .ghiChuGiaoHang(e.getGhiChuGiaoHang())
                .ngayCapNhat(e.getNgayCapNhat())
                .lyDoHuy(e.getDonHangCuaHang().getLyDoHuy())
                .trangThaiTiepTheo(st == null ? List.of() : Arrays.stream(ShipmentStatus.values())
                        .filter(st::canMoveTo)
                        .map(Enum::name)
                        .toList())
                .build();
    }
}
