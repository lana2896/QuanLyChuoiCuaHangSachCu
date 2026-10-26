package com.oldbook.service.notification;

import com.oldbook.constant.auth.AccountStatus;
import com.oldbook.constant.auth.Role;
import com.oldbook.constant.notification.NotificationLevel;
import com.oldbook.dto.catalog.PageResponse;
import com.oldbook.dto.notification.NotificationResponse;
import com.oldbook.entity.identity.Account;
import com.oldbook.entity.notification.Notification;
import com.oldbook.exception.common.BusinessException;
import com.oldbook.repository.notification.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Lưu và đọc thông báo. Lớp này không biết sự kiện nào thì cần báo cho ai:
 * quy tắc đó thuộc về từng khu vực (ví dụ AdminNotificationService cho quản trị viên).
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int TIEU_DE_MAX = 255;
    private static final int NOI_DUNG_MAX = 1000;

    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;

    public record NotificationContent(
            String loai,
            NotificationLevel mucDo,
            String tieuDe,
            String noiDung,
            String duongDan
    ) {
    }

    /**
     * Gửi cùng một thông báo cho mọi tài khoản đang hoạt động của một vai trò.
     * Nếu người thực hiện cũng thuộc vai trò đó, họ vẫn có một bản để tra lại,
     * nhưng bản đó được lưu ở trạng thái đã đọc: không ai cần được báo về việc chính mình vừa làm.
     */
    @Transactional
    public void sendToRole(Role vaiTroNhan, Integer maTKThucHien, NotificationContent noiDung) {

        List<Account> nguoiNhan = notificationRepository.findAccountsByRoleAndStatus(
                vaiTroNhan.name(),
                AccountStatus.HOAT_DONG.name()
        );

        persist(nguoiNhan, maTKThucHien, noiDung);
    }

    /**
     * Gửi cho đúng một tài khoản, ví dụ báo cho chủ chi nhánh rằng cửa hàng của họ đã được duyệt.
     */
    @Transactional
    public void sendToAccount(Account taiKhoanNhan, NotificationContent noiDung) {
        persist(List.of(taiKhoanNhan), null, noiDung);
    }

    private void persist(List<Account> nguoiNhan, Integer maTKThucHien, NotificationContent noiDung) {

        LocalDateTime now = LocalDateTime.now();

        List<Notification> thongBaos = nguoiNhan.stream()
                .map(taiKhoan -> {
                    boolean laNguoiThucHien = taiKhoan.getMaTK().equals(maTKThucHien);
                    return Notification.builder()
                            .taiKhoanNhan(taiKhoan)
                            .loai(noiDung.loai())
                            .mucDo(noiDung.mucDo().name())
                            .tieuDe(cut(noiDung.tieuDe(), TIEU_DE_MAX))
                            .noiDung(cut(noiDung.noiDung(), NOI_DUNG_MAX))
                            .duongDan(noiDung.duongDan())
                            .daDoc(laNguoiThucHien)
                            .ngayDoc(laNguoiThucHien ? now : null)
                            .ngayTao(now)
                            .build();
                })
                .toList();

        List<Notification> daLuu = notificationRepository.saveAll(thongBaos);

        // Kênh đẩy (NotificationPusher) nghe sự kiện này sau khi giao dịch commit.
        eventPublisher.publishEvent(new NotificationSentEvent(
                daLuu.stream()
                        .filter(thongBao -> !thongBao.isDaDoc())
                        .map(thongBao -> new NotificationSentEvent.BanGhi(
                                thongBao.getTaiKhoanNhan().getMaTK(),
                                NotificationResponse.from(thongBao)
                        ))
                        .toList()
        ));
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> danhSach(Integer maTK, int page, int size) {

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        return PageResponse.of(
                notificationRepository
                        .findByTaiKhoanNhan_MaTKOrderByNgayTaoDescMaThongBaoDesc(
                                maTK,
                                PageRequest.of(safePage, safeSize)
                        )
                        .map(NotificationResponse::from)
        );
    }

    @Transactional(readOnly = true)
    public long countUnread(Integer maTK) {
        return notificationRepository.countByTaiKhoanNhan_MaTKAndDaDocFalse(maTK);
    }

    @Transactional
    public void markRead(Integer maTK, Integer maThongBao) {

        Notification thongBao = notificationRepository
                .findByMaThongBaoAndTaiKhoanNhan_MaTK(maThongBao, maTK)
                .orElseThrow(() -> new BusinessException("Thông báo không tồn tại"));

        if (!thongBao.isDaDoc()) {
            thongBao.setDaDoc(true);
            thongBao.setNgayDoc(LocalDateTime.now());
            notificationRepository.save(thongBao);
        }
    }

    @Transactional
    public void markAllAsRead(Integer maTK) {
        notificationRepository.markAllRead(maTK, LocalDateTime.now());
    }

    private static String cut(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max - 1) + "…";
    }
}
