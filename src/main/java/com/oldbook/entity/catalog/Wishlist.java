package com.oldbook.entity.catalog;

import com.oldbook.entity.identity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "yeu_thich")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wishlist {

    @EmbeddedId
    private UserBookKey id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("maND")
    @JoinColumn(name = "ma_nd")
    private User nguoiDung;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("maSach")
    @JoinColumn(name = "ma_sach")
    private Book sach;

    @Column(name = "ngay_them", updatable = false)
    private LocalDateTime ngayThem;

    @PrePersist
    protected void onCreate() {
        this.ngayThem = LocalDateTime.now();
    }
}
