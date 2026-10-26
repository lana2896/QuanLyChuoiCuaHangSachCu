package com.oldbook.entity.payment;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.oldbook.entity.catalog.Store;

@Entity
@Table(name = "khuyen_mai")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_km")
    private Integer maKM;

    // Khuyến mãi có thể thuộc về 1 cửa hàng, hoặc thuộc về Trụ sở (nullable = true)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_ch", referencedColumnName = "ma_ch", nullable = true)
    private Store cuaHang;

    @Column(name = "code", unique = true, nullable = false, length = 50)
    private String code;

    @Column(name = "ten_khuyen_mai", length = 255)
    private String tenKhuyenMai;

    @Column(name = "loai_voucher", length = 50)
    private String loaiVoucher;

    @Column(name = "loai_giam_gia", length = 50)
    private String loaiGiamGia;

    @Column(name = "gia_tri_giam", precision = 15, scale = 2)
    private BigDecimal giaTriGiam;

    @Column(name = "gia_tri_giam_toi_da", precision = 15, scale = 2)
    private BigDecimal giaTriGiamToiDa;

    @Column(name = "gia_tri_don_toi_thieu", precision = 15, scale = 2)
    private BigDecimal giaTriDonToiThieu;

    @Column(name = "so_luong")
    private Integer soLuong;

    @Column(name = "da_su_dung")
    private Integer daSuDung;

    @Column(name = "ngay_bat_dau")
    private LocalDateTime ngayBatDau;

    @Column(name = "ngay_ket_thuc")
    private LocalDateTime ngayKetThuc;

    @Column(name = "trang_thai", length = 50)
    private String trangThai;
}