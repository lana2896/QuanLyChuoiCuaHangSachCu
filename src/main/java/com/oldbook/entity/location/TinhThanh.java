package com.oldbook.entity.location;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tinh_thanh")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TinhThanh {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_tinh")
    private Integer maTinh;

    @Column(name = "ten_tinh", unique = true, nullable = false, length = 100)
    private String tenTinh;

    @Column(name = "loai", nullable = false, length = 20)
    private String loai;
}
