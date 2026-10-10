package com.oldbook.entity.cart;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

import com.oldbook.entity.identity.User;

@Entity
@Table(name = "gio_hang")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_gio_hang")
    private Integer maGioHang;

    // Quan hệ 1-1 với User
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_nd", referencedColumnName = "ma_nd", unique = true, nullable = false)
    private User nguoiDung;

    @Column(name = "ngay_cap_nhat")
    private LocalDateTime ngayCapNhat;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.ngayCapNhat = LocalDateTime.now();
    }
}