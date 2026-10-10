package com.oldbook.entity.auth;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import com.oldbook.entity.identity.NguoiDung;

@Entity
@Table(name = "ma_xac_thuc")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaXacThuc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_xac_thuc")
    private Integer maXacThuc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "ma_nd",
            referencedColumnName = "ma_nd",
            nullable = false
    )
    private NguoiDung nguoiDung;

    @Column(name = "ma_otp", nullable = false, length = 6)
    private String maOtp;

    @Column(name = "loai_xac_thuc", nullable = false, length = 30)
    private String loaiXacThuc;

    @Column(name = "thoi_gian_het_han", nullable = false)
    private LocalDateTime thoiGianHetHan;

    @Column(name = "da_su_dung", nullable = false)
    private Boolean daSuDung;

    @Column(name = "ngay_tao", updatable = false)
    private LocalDateTime ngayTao;

    @PrePersist
    protected void onCreate() {
        this.ngayTao = LocalDateTime.now();
    }
}