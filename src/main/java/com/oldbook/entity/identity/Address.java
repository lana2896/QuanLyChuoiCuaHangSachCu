package com.oldbook.entity.identity;

import com.oldbook.entity.location.PhuongXa;
import com.oldbook.entity.location.TinhThanh;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "dia_chi")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_dia_chi")
    private Integer maDiaChi;

    // Quan hệ Nhiều-1: Nhiều địa chỉ có thể thuộc về một người dùng
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_nd", referencedColumnName = "ma_nd", nullable = false)
    private User nguoiDung;

    @Column(name = "ten_nguoi_nhan", length = 255)
    private String tenNguoiNhan;

    @Column(name = "so_dien_thoai", length = 20)
    private String soDienThoai;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_tinh", referencedColumnName = "ma_tinh", nullable = false)
    private TinhThanh tinh;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_xa", referencedColumnName = "ma_xa", nullable = false)
    private PhuongXa xa;

    @Column(name = "dia_chi_chi_tiet", length = 500)
    private String diaChiChiTiet;

    @Column(name = "la_mac_dinh")
    private Boolean laMacDinh;

    public String getTinhThanh() {
        return tinh.getTenTinh();
    }

    public String getPhuongXa() {
        return xa.getTenXa();
    }

    public String getQuanHuyen() {
        return null;
    }
}
