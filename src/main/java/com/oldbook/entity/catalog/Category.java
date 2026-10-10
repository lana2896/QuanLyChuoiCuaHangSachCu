package com.oldbook.entity.catalog;

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
}