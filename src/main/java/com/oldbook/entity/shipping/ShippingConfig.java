package com.oldbook.entity.shipping;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "cau_hinh_van_chuyen")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_cau_hinh_vc")
    private Integer maCauHinhVC;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_dvvc", referencedColumnName = "ma_dvvc", nullable = false)
    private ShippingCarrier donViVanChuyen;

    @Column(name = "khu_vuc", length = 255)
    private String khuVuc;

    @Column(name = "phi_co_ban", precision = 15, scale = 2)
    private BigDecimal phiCoBan;

    @Column(name = "phi_theo_kg", precision = 15, scale = 2)
    private BigDecimal phiTheoKg;

    @Column(name = "thoi_gian_du_kien")
    private Integer thoiGianDuKien;

    @Column(name = "trang_thai", length = 50)
    private String trangThai;
}