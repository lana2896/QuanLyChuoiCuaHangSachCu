package com.oldbook.entity.system;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "danh_gia_media")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_media")
    private Integer maMedia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_danh_gia", referencedColumnName = "ma_danh_gia", nullable = false)
    private Review danhGia;

    @Column(name = "loai_media", length = 50)
    private String loaiMedia;

    @Column(name = "media_url", length = 500)
    private String mediaUrl;

    @Column(name = "cloudinary_public_id", length = 255)
    private String cloudinaryPublicId;
}
