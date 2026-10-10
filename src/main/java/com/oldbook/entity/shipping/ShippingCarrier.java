package com.oldbook.entity.shipping;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "don_vi_van_chuyen")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingCarrier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_dvvc")
    private Integer maDVVC;

    @Column(name = "ma_code", length = 30)
    private String maCode;

    @Column(name = "ten_don_vi", length = 255)
    private String tenDonVi;

    // Phí vận chuyển áp dụng cho mỗi đơn hàng con khi chọn đơn vị này
    @Column(name = "phi_van_chuyen", precision = 15, scale = 2)
    private BigDecimal phiVanChuyen;

    // Số ngày giao dự kiến
    @Column(name = "thoi_gian_giao_du_kien")
    private Integer thoiGianGiaoDuKien;

    @Column(name = "so_dien_thoai", length = 20)
    private String soDienThoai;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "trang_thai", length = 50)
    private String trangThai;

    @Column(name = "thong_tin_tich_hop", length = 500)
    private String thongTinTichHop;

    @Column(name = "ngay_tao", updatable = false)
    private LocalDate ngayTao;

    @PrePersist
    protected void onCreate() {
        this.ngayTao = LocalDate.now();
    }
}
