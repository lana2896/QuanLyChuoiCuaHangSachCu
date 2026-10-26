package com.oldbook.entity.payment;

import com.oldbook.entity.identity.User;
import com.oldbook.entity.order.Order;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.Length;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Một lần khách chọn chuyển khoản ở trang thanh toán. Trong lúc phiên còn chờ, sách được giữ tạm
 * (đã trừ khỏi tồn kho) nhưng chưa có đơn hàng và giỏ hàng chưa bị đụng tới.
 * Đơn hàng chỉ được tạo khi tiền về, từ đúng yêu cầu đặt hàng đã chụp lại ở đây.
 */
@Entity
@Table(
        name = "phien_thanh_toan",
        indexes = {
                @Index(name = "ix_phien_tt_trang_thai_han", columnList = "trang_thai, het_han_luc")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_phien")
    private Integer maPhien;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_nd", referencedColumnName = "ma_nd", nullable = false)
    private User nguoiMua;

    // Nội dung chuyển khoản khách phải ghi, ví dụ OB7K2M9QXA. Webhook dùng mã này để tìm lại phiên.
    @Column(name = "ma_thanh_toan", nullable = false, unique = true, length = 20)
    private String maThanhToan;

    @Column(name = "so_tien", nullable = false, precision = 15, scale = 2)
    private BigDecimal soTien;

    // Tên của PaymentSessionStatus
    @Column(name = "trang_thai", nullable = false, length = 30)
    private String trangThai;

    @Column(name = "het_han_luc", nullable = false)
    private LocalDateTime hetHanLuc;

    // CreateOrderRequest dạng JSON. Có ghi chú tiếng Việt của khách nên phải là cột Unicode.
    @Nationalized
    @Column(name = "yeu_cau_json", nullable = false, length = Length.LONG32)
    private String yeuCauJson;

    // Danh sách [{maSach, soLuong}] đang giữ, để trả đúng số đó về kho khi phiên kết thúc.
    @Column(name = "giu_hang_json", nullable = false, length = 4000)
    private String giuHangJson;

    // Đã cộng lại tồn kho cho các sách đang giữ hay chưa; chặn việc trả kho hai lần.
    @Column(name = "da_tra_kho", nullable = false)
    private boolean daTraKho;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_dh", referencedColumnName = "ma_dh")
    private Order donHang;

    @Column(name = "so_tien_nhan", precision = 15, scale = 2)
    private BigDecimal soTienNhan;

    // Tài khoản hoàn ưu tiên khách chọn khi hủy đơn (trống thì dùng tài khoản người chuyển trong giao dịch ngân hàng).
    @Column(name = "hoan_ma_ngan_hang", length = 20)
    private String hoanMaNganHang;

    @Column(name = "hoan_so_tai_khoan", length = 50)
    private String hoanSoTaiKhoan;

    @org.hibernate.annotations.Nationalized
    @Column(name = "hoan_ten_chu_tai_khoan", length = 255)
    private String hoanTenChuTaiKhoan;


    @Column(name = "ngay_tao", nullable = false, updatable = false)
    private LocalDateTime ngayTao;

    @Column(name = "ngay_cap_nhat")
    private LocalDateTime ngayCapNhat;

    @PrePersist
    protected void onCreate() {
        if (this.ngayTao == null) {
            this.ngayTao = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.ngayCapNhat = LocalDateTime.now();
    }
}
