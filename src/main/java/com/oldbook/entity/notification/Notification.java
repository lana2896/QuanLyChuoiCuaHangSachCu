package com.oldbook.entity.notification;

import com.oldbook.entity.identity.Account;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;

/**
 * Một thông báo gửi cho một tài khoản. Cùng một sự kiện gửi cho nhiều người thì mỗi người một dòng,
 * nên trạng thái đã đọc là của riêng từng tài khoản.
 */
@Entity
@Table(
        name = "thong_bao",
        indexes = {
                @Index(name = "ix_thong_bao_nguoi_nhan", columnList = "ma_tk_nhan, da_doc, ngay_tao")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_thong_bao")
    private Integer maThongBao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_tk_nhan", referencedColumnName = "ma_tk", nullable = false)
    private Account taiKhoanNhan;

    // Mã loại sự kiện, ví dụ KHOA_TAI_KHOAN. Giao diện dùng để chọn biểu tượng.
    @Column(name = "loai", nullable = false, length = 100)
    private String loai;

    // Tên của NotificationLevel
    @Column(name = "muc_do", nullable = false, length = 50)
    private String mucDo;

    @Nationalized
    @Column(name = "tieu_de", nullable = false, length = 255)
    private String tieuDe;

    // Không dùng kiểu TEXT: SQL Server không cho dùng lower() hay so sánh trên cột TEXT.
    @Nationalized
    @Column(name = "noi_dung", length = 1000)
    private String noiDung;

    // Trang cần mở để xử lý, ví dụ /admin/accounts?tuKhoa=...
    @Column(name = "duong_dan", length = 500)
    private String duongDan;

    @Column(name = "da_doc", nullable = false)
    private boolean daDoc;

    @Column(name = "ngay_doc")
    private LocalDateTime ngayDoc;

    @Column(name = "ngay_tao", nullable = false, updatable = false)
    private LocalDateTime ngayTao;

    @PrePersist
    protected void onCreate() {
        if (this.ngayTao == null) {
            this.ngayTao = LocalDateTime.now();
        }
    }
}
