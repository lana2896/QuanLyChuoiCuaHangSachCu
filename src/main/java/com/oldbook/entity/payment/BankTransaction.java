package com.oldbook.entity.payment;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Một giao dịch trên tài khoản ngân hàng của trụ sở do Casso báo về. Bảng này là sổ đối soát:
 * giao dịch nào cũng được ghi lại, kể cả khi không khớp phiên thanh toán nào.
 */
@Entity
@Table(name = "giao_dich_ngan_hang")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_giao_dich")
    private Integer maGiaoDich;

    // Mã do Casso cấp. Duy nhất, nên Casso gửi lại cùng một giao dịch cũng chỉ được xử lý một lần.
    @Column(name = "ma_giao_dich_cong", nullable = false, unique = true, length = 100)
    private String maGiaoDichCong;

    @Column(name = "ma_tham_chieu", length = 100)
    private String maThamChieu;

    // Dương là tiền vào, âm là tiền ra.
    @Column(name = "so_tien", nullable = false, precision = 15, scale = 2)
    private BigDecimal soTien;

    @Nationalized
    @Column(name = "noi_dung", length = 1000)
    private String noiDung;

    @Column(name = "thoi_gian_giao_dich")
    private LocalDateTime thoiGianGiaoDich;

    @Column(name = "so_tai_khoan_nhan", length = 50)
    private String soTaiKhoanNhan;

    // Tài khoản của người chuyển, nếu ngân hàng có gửi kèm. Dùng để điền sẵn khi phải hoàn tiền.
    @Nationalized
    @Column(name = "ten_nguoi_chuyen", length = 255)
    private String tenNguoiChuyen;

    @Column(name = "so_tai_khoan_chuyen", length = 50)
    private String soTaiKhoanChuyen;

    @Column(name = "ma_ngan_hang_chuyen", length = 20)
    private String maNganHangChuyen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_phien", referencedColumnName = "ma_phien")
    private PaymentSession phien;

    // Tên của ReconciliationResult
    @Column(name = "ket_qua", nullable = false, length = 30)
    private String ketQua;

    @Nationalized
    @Column(name = "ghi_chu", length = 500)
    private String ghiChu;

    @Column(name = "ngay_nhan", nullable = false, updatable = false)
    private LocalDateTime ngayNhan;

    @PrePersist
    protected void onCreate() {
        if (this.ngayNhan == null) {
            this.ngayNhan = LocalDateTime.now();
        }
    }
}
