package com.oldbook.entity.order;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

import com.oldbook.entity.catalog.Book;

@Entity
@Table(name = "chi_tiet_don_hang")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_ctdh")
    private Integer maCTDH;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_dhch", referencedColumnName = "ma_dhch", nullable = false)
    private StoreOrder donHangCuaHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_sach", referencedColumnName = "ma_sach", nullable = false)
    private Book sach;

    @Column(name = "so_luong", nullable = false)
    private Integer soLuong;

    @Column(name = "don_gia", precision = 15, scale = 2)
    private BigDecimal donGia;

    @Column(name = "thanh_tien", precision = 15, scale = 2)
    private BigDecimal thanhTien;

    @Column(name = "ty_le_chiet_khau", precision = 5, scale = 2)
    private BigDecimal tyLeChietKhau;

    @Column(name = "tien_chiet_khau", precision = 15, scale = 2)
    private BigDecimal tienChietKhau;
}
