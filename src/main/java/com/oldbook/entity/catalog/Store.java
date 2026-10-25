package com.oldbook.entity.catalog;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import com.oldbook.entity.identity.User;
import com.oldbook.entity.location.PhuongXa;
import com.oldbook.entity.location.TinhThanh;

@Entity
@Table(name = "cua_hang")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_ch")
    private Integer maCH;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_nd_chu_shop", referencedColumnName = "ma_nd", unique = true, nullable = false)
    private User chuShop;

    @Column(name = "ten_cua_hang", unique = true, nullable = false, length = 255)
    private String tenCuaHang;

    @Column(name = "mo_ta", length = 1000)
    private String moTa;

    @Column(name = "so_chi_nhanh", unique = true)
    private Integer soChiNhanh;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_tinh", referencedColumnName = "ma_tinh", nullable = false)
    private TinhThanh tinh;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_xa", referencedColumnName = "ma_xa", nullable = false)
    private PhuongXa xa;

    @Column(name = "dia_chi_chi_tiet", nullable = false, length = 500)
    private String diaChiChiTiet;

    @Column(name = "so_dien_thoai_ch", length = 20)
    private String soDienThoaiCH;

    @Column(name = "trang_thai_duyet", length = 50)
    private String trangThaiDuyet;

    @Column(name = "ngay_tao", updatable = false)
    private LocalDateTime ngayTao;

    public String getDiaChiCH() {
        return diaChiChiTiet + ", " + xa.getTenXa() + ", " + tinh.getTenTinh();
    }

    @PrePersist
    protected void onCreate() {
        this.ngayTao = LocalDateTime.now();
    }
}