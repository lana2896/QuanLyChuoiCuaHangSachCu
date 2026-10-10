package com.oldbook.entity.catalog;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sach")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_sach")
    private Integer maSach;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_ch", referencedColumnName = "ma_ch", nullable = false)
    private Store cuaHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_dm", referencedColumnName = "ma_dm", nullable = false)
    private Category danhMuc;

    @Column(name = "ten_sach", nullable = false, length = 255)
    private String tenSach;

    @Column(name = "tac_gia", length = 255)
    private String tacGia;

    @Column(name = "nha_xuat_ban", length = 255)
    private String nhaXuatBan;

    @Column(name = "nam_xuat_ban")
    private Integer namXuatBan;

    @Column(name = "gia_bia", precision = 15, scale = 2)
    private BigDecimal giaBia;

    @Column(name = "gia_ban_cu", precision = 15, scale = 2)
    private BigDecimal giaBanCu;

    @Column(name = "do_moi_percent", precision = 5, scale = 2)
    private BigDecimal doMoiPercent;

    @Column(name = "tinh_trang_vat_ly", length = 255)
    private String tinhTrangVatLy;

    @Column(name = "mo_ta_chi_tiet", columnDefinition = "TEXT")
    private String moTaChiTiet;

    @Column(name = "so_luong_ton")
    private Integer soLuongTon;

    @Column(name = "hinh_anh_url", length = 500)
    private String hinhAnhUrl;

    @Column(name = "trang_thai_duyet", length = 50)
    private String trangThaiDuyet;

    @Column(name = "trang_thai_ban", length = 50)
    private String trangThaiBan;

    @Column(name = "ly_do_tu_choi", length = 500)
    private String lyDoTuChoi;

    @Column(name = "ngay_tao", updatable = false)
    private LocalDateTime ngayTao;

    @PrePersist
    protected void onCreate() {
        this.ngayTao = LocalDateTime.now();
    }
}