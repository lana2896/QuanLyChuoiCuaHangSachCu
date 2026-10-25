package com.oldbook.entity.catalog;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "anh_sach")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_anh")
    private Integer maAnh;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_sach", referencedColumnName = "ma_sach", nullable = false)
    private Book sach;

    @Column(name = "hinh_anh_url", nullable = false, length = 500)
    private String hinhAnhUrl;

    @Column(name = "thu_tu", nullable = false)
    private Integer thuTu;
}
