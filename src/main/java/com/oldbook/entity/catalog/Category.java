package com.oldbook.entity.catalog;

import com.oldbook.constant.catalog.SaleStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "danh_muc")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_dm")
    private Integer maDM;

    @Column(name = "ten_danh_muc", unique = true, nullable = false, length = 255)
    private String tenDanhMuc;

    @Column(name = "mo_ta", length = 500)
    private String moTa;

    @Builder.Default
    @Column(name = "trang_thai_ban", nullable = false, length = 20,
            columnDefinition = "VARCHAR(20) DEFAULT 'DANG_BAN'")
    private String trangThaiBan = SaleStatus.DANG_BAN.name();
}