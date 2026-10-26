package com.oldbook.entity.payment;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.oldbook.entity.catalog.Category;

@Entity
@Table(name = "cau_hinh_chiet_khau")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommissionConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_cau_hinh_chiet_khau")
    private Integer maCauHinhChietKhau;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_dm", referencedColumnName = "ma_dm", nullable = false)
    private Category danhMuc;

    @Column(name = "ty_le_chiet_khau", precision = 5, scale = 2)
    private BigDecimal tyLeChietKhau;

    @Column(name = "trang_thai", length = 50)
    private String trangThai;

    @Column(name = "ngay_bat_dau")
    private LocalDateTime ngayBatDau;

    @Column(name = "ngay_ket_thuc")
    private LocalDateTime ngayKetThuc;

    // Tài khoản Quản lý đặt tỷ lệ này; trống với cấu hình có từ trước.
    @Column(name = "ma_tk_cap_nhat")
    private Integer maTKCapNhat;

    @Nationalized
    @Column(name = "ghi_chu", length = 500)
    private String ghiChu;
}