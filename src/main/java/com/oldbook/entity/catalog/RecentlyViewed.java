package com.oldbook.entity.catalog;

import com.oldbook.entity.identity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sach_da_xem")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecentlyViewed {

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

    @Column(name = "lan_xem_gan_nhat")
    private LocalDateTime lanXemGanNhat;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.lanXemGanNhat = LocalDateTime.now();
    }
}
