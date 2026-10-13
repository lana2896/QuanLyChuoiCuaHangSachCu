package com.oldbook.entity.payment;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.oldbook.entity.order.Order;
import com.oldbook.entity.order.StoreOrder;

@Entity
@Table(name = "thanh_toan")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_giao_dich")
    private Integer maGiaoDich;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_dh", referencedColumnName = "ma_dh", nullable = false)
    private Order donHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_dhch", referencedColumnName = "ma_dhch", nullable = true)
    private StoreOrder donHangCuaHang;

    @Column(name = "cong_thanh_toan", length = 50)
    private String congThanhToan;

    @Column(name = "loai_giao_dich", length = 50)
    private String loaiGiaoDich;

    @Column(name = "ma_giao_dich_cong", length = 255)
    private String maGiaoDichCong;

    @Column(name = "so_tien", precision = 15, scale = 2)
    private BigDecimal soTien;

    @Column(name = "trang_thai_giao_dich", length = 50)
    private String trangThaiGiaoDich;

    @Column(name = "thoi_gian_tt")
    private LocalDateTime thoiGianTT;
}