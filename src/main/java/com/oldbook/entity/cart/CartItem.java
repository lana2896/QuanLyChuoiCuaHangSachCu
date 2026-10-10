package com.oldbook.entity.cart;

import com.oldbook.entity.catalog.Book;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "chi_tiet_gio_hang",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_gio_hang_sach",
                        columnNames = {"ma_gio_hang", "ma_sach"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_ct_gio_hang")
    private Integer maCTGioHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_gio_hang", referencedColumnName = "ma_gio_hang", nullable = false)
    private Cart gioHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_sach", referencedColumnName = "ma_sach", nullable = false)
    private Book sach;

    @Column(name = "so_luong", nullable = false)
    private Integer soLuong;
}