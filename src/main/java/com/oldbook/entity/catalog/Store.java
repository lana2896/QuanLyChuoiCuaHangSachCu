package com.oldbook.entity.catalog;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import com.oldbook.entity.identity.NguoiDung;

@Entity
@Table(name = "cua_hang")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CuaHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_ch")
    private Integer maCH;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_nd_chu_shop", referencedColumnName = "ma_nd", unique = true, nullable = false)
    private NguoiDung chuShop;

    @Column(name = "ten_cua_hang", unique = true, nullable = false, length = 255)
    private String tenCuaHang;

    @Column(name = "mo_ta", length = 1000)
    private String moTa;

    @Column(name = "dia_chi_ch", length = 500)
    private String diaChiCH;

    @Column(name = "so_dien_thoai_ch", length = 20)
    private String soDienThoaiCH;

    @Column(name = "trang_thai_duyet", length = 50)
    private String trangThaiDuyet;

    @Column(name = "ngay_tao", updatable = false)
    private LocalDateTime ngayTao;

    @PrePersist
    protected void onCreate() {
        this.ngayTao = LocalDateTime.now();
    }
}