package com.oldbook.entity.catalog;

import com.oldbook.constant.catalog.SaleStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tua_sach")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookTitle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_tua_sach")
    private Integer maTuaSach;

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

    @Column(name = "gia_goc", nullable = false, precision = 15, scale = 2)
    private BigDecimal giaGoc;

    @Column(name = "mo_ta_chi_tiet", columnDefinition = "TEXT")
    private String moTaChiTiet;

    @Column(name = "hinh_anh_url", length = 500)
    private String hinhAnhUrl;

    @Builder.Default
    @Column(name = "trang_thai_ban", nullable = false, length = 20,
            columnDefinition = "VARCHAR(20) DEFAULT 'DANG_BAN'")
    private String trangThaiBan = SaleStatus.DANG_BAN.name();

    @Column(name = "ngay_tao", updatable = false)
    private LocalDateTime ngayTao;

    @PrePersist
    protected void onCreate() {
        this.ngayTao = LocalDateTime.now();
    }
}
