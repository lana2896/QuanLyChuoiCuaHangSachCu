package com.oldbook.entity.order;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.oldbook.entity.catalog.Store;
import com.oldbook.entity.shipping.ShippingCarrier;

@Entity
@Table(
        name = "don_hang_cua_hang",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_don_hang_cua_hang",
                        columnNames = {"ma_dh", "ma_ch"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_dhch")
    private Integer maDHCH;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_dh", referencedColumnName = "ma_dh", nullable = false)
    private Order donHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_ch", referencedColumnName = "ma_ch", nullable = false)
    private Store cuaHang;

    // Đơn vị vận chuyển do khách chọn riêng cho đơn hàng con này
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_dvvc", referencedColumnName = "ma_dvvc")
    private ShippingCarrier donViVanChuyen;

    @Column(name = "giam_gia_shop", precision = 15, scale = 2)
    private BigDecimal giamGiaShop;

    @Column(name = "giam_gia_san_phan_bo", precision = 15, scale = 2)
    private BigDecimal giamGiaSanPhanBo;

    @Column(name = "tien_hang_ch", precision = 15, scale = 2)
    private BigDecimal tienHangCH;

    @Column(name = "phi_ship_ch", precision = 15, scale = 2)
    private BigDecimal phiShipCH;

    @Column(name = "commission_tien", precision = 15, scale = 2)
    private BigDecimal commissionTien;

    @Column(name = "tong_tien_shop_order", precision = 15, scale = 2)
    private BigDecimal tongTienShopOrder;

    @Column(name = "trang_thai_don_hang", length = 50)
    private String trangThaiDonHang;

    @Column(name = "trang_thai_thanh_toan_shop", length = 50)
    private String trangThaiThanhToanShop;

    @Column(name = "tien_hoan_tra", precision = 15, scale = 2)
    private BigDecimal tienHoanTra;

    // Doanh thu thực nhận của cửa hàng = tiền hàng - giảm giá shop - hoa hồng. Chỉ có khi đã giao thành công.
    @Column(name = "doanh_thu_shop", precision = 15, scale = 2)
    private BigDecimal doanhThuShop;

    @Column(name = "ngay_giao")
    private LocalDateTime ngayGiao;

    @Column(name = "ngay_hoan_tra")
    private LocalDateTime ngayHoanTra;

    @Column(name = "ngay_cap_nhat")
    private LocalDateTime ngayCapNhat;

    // Thời điểm chủ cửa hàng xác nhận đơn
    @Column(name = "ngay_xac_nhan")
    private LocalDateTime ngayXacNhan;

    // Ghi chú của khách gửi riêng cho cửa hàng này
    @Column(name = "ghi_chu", columnDefinition = "TEXT")
    private String ghiChu;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.ngayCapNhat = LocalDateTime.now();
    }
}
