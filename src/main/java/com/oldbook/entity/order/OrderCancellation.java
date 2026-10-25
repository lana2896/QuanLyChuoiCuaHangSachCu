package com.oldbook.entity.order;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import com.oldbook.entity.identity.User;

@Entity
@Table(name = "yeu_cau_huy_don")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCancellation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_yeu_cau")
    private Integer maYeuCau;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_dhch", referencedColumnName = "ma_dhch", nullable = false)
    private StoreOrder donHangCuaHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_nd_yeu_cau", referencedColumnName = "ma_nd", nullable = false)
    private User nguoiDung;

    @Column(name = "ly_do", columnDefinition = "TEXT")
    private String lyDo;

    @Column(name = "ly_do_phan_hoi", length = 1000)
    private String lyDoPhanHoi;

    @Column(name = "trang_thai_yeu_cau", length = 50)
    private String trangThaiYeuCau;

    @Column(name = "ngay_gui", updatable = false)
    private LocalDateTime ngayGui;

    @Column(name = "ngay_xu_ly")
    private LocalDateTime ngayXuLy;

    @PrePersist
    protected void onCreate() {
        this.ngayGui = LocalDateTime.now();
    }
}