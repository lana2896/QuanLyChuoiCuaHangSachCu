package com.oldbook.entity.identity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "tai_khoan",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_tai_khoan_nguoi_dung_vai_tro",
                        columnNames = {"ma_nd", "vai_tro"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_tk")
    private Integer maTK;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "ma_nd",
            referencedColumnName = "ma_nd",
            nullable = false
    )
    private User nguoiDung;

    @Column(name = "vai_tro", nullable = false, length = 50)
    private String vaiTro;

    @Column(name = "trang_thai", nullable = false, length = 50)
    private String trangThai;

    @Column(name = "ngay_tao", updatable = false)
    private LocalDateTime ngayTao;

    @PrePersist
    protected void onCreate() {
        this.ngayTao = LocalDateTime.now();
    }
}