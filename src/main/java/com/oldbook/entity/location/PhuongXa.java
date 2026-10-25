package com.oldbook.entity.location;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "phuong_xa", uniqueConstraints = @UniqueConstraint(columnNames = {"ma_tinh", "ten_xa"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhuongXa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_xa")
    private Integer maXa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_tinh", referencedColumnName = "ma_tinh", nullable = false)
    private TinhThanh tinh;

    @Column(name = "ten_xa", nullable = false, length = 100)
    private String tenXa;
}
