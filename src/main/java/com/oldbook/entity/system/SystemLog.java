package com.oldbook.entity.system;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import com.oldbook.entity.identity.TaiKhoan;

@Entity
@Table(name = "nhat_ky_he_thong")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NhatKyHeThong {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_nhat_ky")
    private Integer maNhatKy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_tk", referencedColumnName = "ma_tk", nullable = false)
    private TaiKhoan taiKhoan;

    @Column(name = "hanh_dong", length = 255)
    private String hanhDong;

    @Column(name = "loai_doi_tuong", length = 100)
    private String loaiDoiTuong;

    @Column(name = "ma_doi_tuong")
    private Integer maDoiTuong;

    @Column(name = "mo_ta", columnDefinition = "TEXT")
    private String moTa;

    @Column(name = "dia_chi_ip", length = 50)
    private String diaChiIp;

    @Column(name = "thoi_gian", updatable = false)
    private LocalDateTime thoiGian;

    @PrePersist
    protected void onCreate() {
        this.thoiGian = LocalDateTime.now();
    }
}