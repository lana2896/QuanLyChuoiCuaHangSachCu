package com.oldbook.entity.inventory;

import com.oldbook.entity.catalog.Book;
import com.oldbook.entity.catalog.Store;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;

/**
 * Phiếu điều chuyển sách giữa hai chi nhánh trong chuỗi.
 * Chi nhánh gửi xuất một số cuốn của một bản sách (sachNguon); khi chi nhánh nhận xác nhận,
 * số cuốn đó được cộng vào bản sách cùng tựa, cùng độ mới ở chi nhánh nhận (sachDich),
 * hoặc tạo bản sách mới nếu chi nhánh nhận chưa có.
 */
@Entity
@Table(name = "phieu_dieu_chuyen")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_phieu")
    private Integer maPhieu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_sach_nguon", referencedColumnName = "ma_sach", nullable = false)
    private Book sachNguon;

    // Bản sách ở chi nhánh nhận; chỉ có sau khi phiếu được nhận.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_sach_dich", referencedColumnName = "ma_sach")
    private Book sachDich;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_ch_nguon", referencedColumnName = "ma_ch", nullable = false)
    private Store cuaHangNguon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_ch_dich", referencedColumnName = "ma_ch", nullable = false)
    private Store cuaHangDich;

    @Column(name = "so_luong", nullable = false)
    private Integer soLuong;

    @Column(name = "trang_thai", nullable = false, length = 30)
    private String trangThai;

    @Nationalized
    @Column(name = "ghi_chu", length = 500)
    private String ghiChu;

    @Nationalized
    @Column(name = "ly_do_tu_choi", length = 500)
    private String lyDoTuChoi;

    @Column(name = "ngay_tao", updatable = false)
    private LocalDateTime ngayTao;

    @Column(name = "ngay_xu_ly")
    private LocalDateTime ngayXuLy;

    @PrePersist
    protected void onCreate() {
        this.ngayTao = LocalDateTime.now();
    }
}
