package com.oldbook.entity.payment;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.oldbook.entity.identity.User;
import com.oldbook.entity.order.Order;
import com.oldbook.entity.order.StoreOrder;

@Entity
@Table(name = "su_dung_voucher")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_sdv")
    private Integer maSDV;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_km", referencedColumnName = "ma_km", nullable = false)
    private Promotion khuyenMai;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_dh", referencedColumnName = "ma_dh", nullable = false)
    private Order donHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_dhch", referencedColumnName = "ma_dhch", nullable = true)
    private StoreOrder donHangCuaHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_nd", referencedColumnName = "ma_nd", nullable = false)
    private User nguoiDung;

    @Column(name = "gia_tri_giam_thuc_te", precision = 15, scale = 2)
    private BigDecimal giaTriGiamThucTe;

    @Column(name = "ngay_su_dung", updatable = false)
    private LocalDateTime ngaySuDung;

    @PrePersist
    protected void onCreate() {
        this.ngaySuDung = LocalDateTime.now();
    }
}