package com.oldbook.entity.catalog;

import com.oldbook.constant.catalog.SaleStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "sach")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_sach")
    private Integer maSach;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_ch", referencedColumnName = "ma_ch", nullable = false)
    private Store cuaHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_tua_sach", referencedColumnName = "ma_tua_sach", nullable = false)
    private BookTitle tuaSach;

    @Column(name = "gia_ban_cu", nullable = false, precision = 15, scale = 2)
    private BigDecimal giaBanCu;

    @Column(name = "do_moi_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal doMoiPercent;

    @Column(name = "tinh_trang_vat_ly", length = 255)
    private String tinhTrangVatLy;

    @Column(name = "so_luong_ton")
    private Integer soLuongTon;

    @Column(name = "trang_thai_duyet", length = 50)
    private String trangThaiDuyet;

    @Column(name = "trang_thai_ban", length = 50)
    private String trangThaiBan;

    @Column(name = "ly_do_tu_choi", length = 500)
    private String lyDoTuChoi;

    @Column(name = "ngay_tao", updatable = false)
    private LocalDateTime ngayTao;

    @PrePersist
    protected void onCreate() {
        this.ngayTao = LocalDateTime.now();
        tinhGiaBan();
    }

    @PreUpdate
    protected void onUpdate() {
        tinhGiaBan();
    }

    public void tinhGiaBan() {
        if (tuaSach != null && tuaSach.getGiaGoc() != null && doMoiPercent != null) {
            this.giaBanCu = tuaSach.getGiaGoc().multiply(doMoiPercent)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
    }

    public boolean tuaSachVaDanhMucDangBan() {
        return tuaSach != null
                && SaleStatus.DANG_BAN.name().equals(tuaSach.getTrangThaiBan())
                && tuaSach.getDanhMuc() != null
                && SaleStatus.DANG_BAN.name().equals(tuaSach.getDanhMuc().getTrangThaiBan());
    }

    public Category getDanhMuc() {
        return tuaSach.getDanhMuc();
    }

    public String getTenSach() {
        return tuaSach.getTenSach();
    }

    public String getTacGia() {
        return tuaSach.getTacGia();
    }

    public String getNhaXuatBan() {
        return tuaSach.getNhaXuatBan();
    }

    public Integer getNamXuatBan() {
        return tuaSach.getNamXuatBan();
    }

    public BigDecimal getGiaBia() {
        return tuaSach.getGiaGoc();
    }

    public String getMoTaChiTiet() {
        return tuaSach.getMoTaChiTiet();
    }

    public String getHinhAnhUrl() {
        return tuaSach.getHinhAnhUrl();
    }
}