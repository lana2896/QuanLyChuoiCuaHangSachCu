package com.oldbook.entity.shipping;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Nhật ký các lần cập nhật trạng thái giao hàng nhận từ đơn vị vận chuyển. */
@Entity
@Table(
        name = "lich_su_van_chuyen",
        indexes = @Index(name = "idx_lich_su_vc_van_chuyen", columnList = "ma_van_chuyen")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentHistory {

    /** Giá trị của cột nguon: sự kiện do hệ thống tự ghi (ví dụ khi cửa hàng xác nhận đơn). */
    public static final String SOURCE_SYSTEM = "HE_THONG";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_lich_su")
    private Integer maLichSu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_van_chuyen", referencedColumnName = "ma_van_chuyen", nullable = false)
    private Shipment vanChuyen;

    @Column(name = "trang_thai", length = 50, nullable = false)
    private String trangThai;

    // Mã trạng thái gốc do đơn vị vận chuyển trả về (để đối soát)
    @Column(name = "trang_thai_goc", length = 100)
    private String trangThaiGoc;

    @Column(name = "ghi_chu", length = 1000)
    private String ghiChu;

    // POLLING (tra cứu API) | DOI_TAC (cổng /shipping/{maCode}) | HE_THONG
    @Column(name = "nguon", length = 30)
    private String nguon;

    // Thời điểm xảy ra sự kiện theo đơn vị vận chuyển (nếu có), mặc định là lúc nhận
    @Column(name = "thoi_gian")
    private LocalDateTime thoiGian;

    @PrePersist
    protected void onCreate() {
        if (this.thoiGian == null) {
            this.thoiGian = LocalDateTime.now();
        }
    }
}
